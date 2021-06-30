import tensorflow as tf

from . import model


def top_k_logits(logits, k):
    if k == 0:
        # no truncation
        return logits

    def _top_k():
        values, _ = tf.nn.top_k(logits, k=k)
        min_values = values[:, -1, tf.newaxis]
        return tf.compat.v1.where(
            logits < min_values,
            tf.ones_like(logits, dtype=logits.dtype) * -1e10,
            logits,
        )
    return tf.cond(
        pred=tf.equal(k, 0),
        true_fn=lambda: logits,
        false_fn=lambda: _top_k(),
    )


def top_p_logits(logits, p):
    with tf.compat.v1.variable_scope('top_p_logits'):
        logits_sort = tf.sort(logits, direction='DESCENDING')
        probs_sort = tf.nn.softmax(logits_sort)
        probs_sums = tf.cumsum(probs_sort, axis=1, exclusive=True)
        logits_masked = tf.compat.v1.where(probs_sums < p, logits_sort, tf.ones_like(
            logits_sort)*1000)  # [batchsize, vocab]
        min_logits = tf.reduce_min(input_tensor=logits_masked, axis=1, keepdims=True)  # [batchsize, 1]
        return tf.compat.v1.where(
            logits < min_logits,
            tf.ones_like(logits, dtype=logits.dtype) * -1e10,
            logits,
        )


def sample_sequence(*, hparams, max_length, context=None, context_output=None,
                    start_token=None, end_tokens=None, batch_size=None,
                    temperature=1, top_k=0, top_p=0.0):
    if start_token is None:
        assert context is not None, 'Specify exactly one of start_token and context!'
    else:
        assert context is None, 'Specify exactly one of start_token and context!'
        context = tf.fill([batch_size, 1], start_token)

    def step(hparams, tokens, past=None):
        lm_output = model.model(hparams=hparams, X=tokens,
                                past=past, reuse=tf.compat.v1.AUTO_REUSE)

        logits = lm_output['logits'][:, :, :hparams.n_vocab]
        presents = lm_output['present']
        presents.set_shape(model.past_shape(
            hparams=hparams, batch_size=batch_size))
        return {
            'logits': logits,
            'presents': presents,
        }

    with tf.compat.v1.name_scope('sample_sequence'):
        # Don't feed the last context token -- leave that to the loop below
        # TODO: Would be slightly faster if we called step on the entire context,
        # rather than leaving the last token transformer calculation to the while loop.
        context_presents = tf.cond(tf.shape(context_output)[-2] > 0,
                                   lambda: context_output,
                                   lambda: step(hparams, context[:, :-1])['presents'],
                                   )
        end_tokens = tf.tile(tf.expand_dims(end_tokens, axis=0), [batch_size, 1])

        def body(end_mask, past, prev, output, output_prob):
            next_outputs = step(hparams, prev[:, tf.newaxis], past=past)
            logits = next_outputs['logits'][:, -1, :]
            probs = tf.nn.softmax(logits)

            if temperature == 0:
                logits = tf.map_fn(fn=lambda logit_tensor: logit_tensor / tf.random.uniform((1,), minval=.69, maxval=.91, dtype=tf.dtypes.float32),
                                   elems=logits,
                                   back_prop=False,
                                   dtype=tf.float32)
            else:
                logits = logits / tf.to_float(temperature)

            if top_p > 0.0:
                logits = top_p_logits(logits, p=top_p)
            else:
                logits = top_k_logits(logits, k=top_k)
            samples = tf.random.categorical(
                logits, num_samples=1, dtype=tf.int32)
            token_id = tf.stack([tf.range(tf.shape(samples)[0]), samples[:, 0]], axis=1)
            prob = tf.expand_dims(tf.gather_nd(probs, token_id), axis=1)
            return [
                tf.logical_or(end_mask, tf.reduce_any(tf.equal(samples, end_tokens), axis=1)),
                tf.concat([past, next_outputs['presents']], axis=-2),
                tf.squeeze(samples, axis=[1]),
                tf.concat([output, samples], axis=1),
                tf.concat([output_prob, prob], axis=1),
            ]

        def cond(end_mask, past, prev, output, output_prob):
            return tf.logical_not(tf.reduce_all(end_mask))

        _, _, _, tokens, probs = tf.while_loop(
            cond=cond, body=body,
            maximum_iterations=max_length,
            loop_vars=[
                tf.repeat(tf.constant(False), repeats=batch_size),
                context_presents,
                context[:, -1],
                tf.zeros([batch_size, 0], dtype=tf.int32),
                tf.zeros([batch_size, 0]),
            ],
            shape_invariants=[
                tf.TensorShape([batch_size]),
                tf.TensorShape(model.past_shape(
                    hparams=hparams, batch_size=batch_size)),
                tf.TensorShape([batch_size]),
                tf.TensorShape([batch_size, None]),
                tf.TensorShape([batch_size, None]),
            ],
            back_prop=False,
        )

        return context_presents, tokens, probs

def probability(hparams, context=None, context_output=None, suggestion=None, end_tokens=None, batch_size=None):
    def step(hparams, tokens, past=None):
        lm_output = model.model(hparams=hparams, X=tokens,
                                past=past, reuse=tf.compat.v1.AUTO_REUSE)

        logits = lm_output['logits'][:, :, :hparams.n_vocab]
        presents = lm_output['present']
        presents.set_shape(model.past_shape(
            hparams=hparams, batch_size=batch_size))
        return {
            'logits': logits,
            'presents': presents,
        }

    with tf.compat.v1.name_scope('probability'):
        context_presents = tf.cond(tf.shape(context_output)[-2] > 0,
                                   lambda: context_output,
                                   lambda: step(hparams, context[:, :-1])['presents'],
                                   )

        def body(i, past, prev, output):
            next_outputs = step(hparams, prev[:, tf.newaxis], past=past)
            logits = next_outputs['logits'][:, -1, :]
            probs = tf.nn.softmax(logits)

            prob = tf.cond(
                tf.less(i, tf.shape(suggestion)[1]),
                lambda: tf.expand_dims(tf.gather_nd(
                    probs,
                    tf.stack([tf.range(tf.shape(suggestion)[0]), suggestion[:, i]], axis=1),    # Token id
                ), axis=1),
                lambda: tf.reduce_sum(tf.gather(probs, end_tokens, axis=1), axis=1, keepdims=True),
            )
            return [
                i + 1,
                tf.concat([past, next_outputs['presents']], axis=-2),
                tf.cond(
                    tf.less(i, tf.shape(suggestion)[1]),
                    lambda: suggestion[:, i],   # Suggestion token
                    lambda: tf.zeros([batch_size], dtype=tf.int32),
                ),
                tf.concat([output, prob], axis=1),
                ]

        def cond(*args):
            return True

        _, _, _, probs = tf.while_loop(
            cond=cond, body=body,
            maximum_iterations=tf.shape(suggestion)[1] + 1,
            loop_vars=[
                tf.constant(0, dtype=tf.int32),
                context_presents,
                context[:, -1],
                tf.zeros([batch_size, 0]),
            ],
            shape_invariants=[
                tf.TensorShape([]),
                tf.TensorShape(model.past_shape(
                    hparams=hparams, batch_size=batch_size)),
                tf.TensorShape([batch_size]),
                tf.TensorShape([batch_size, None]),
            ],
            back_prop=False,
        )

        return context_presents, probs