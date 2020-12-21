import os
import random
from pathlib import Path
from shutil import copyfile


def rand_fold(n_folds):
    return random.randint(0, n_folds-1)


if __name__ == "__main__":
    data_forms = ['excode', 'java']
    projects = ['netbeans', 'eclipse']
    data_types = ['train', 'test', 'validate']
    version = '3'
    n_folds = 10

    for data_form in data_forms:
        data_classform_folder_with_type = '../../../../../data_v' + version + '/' + 'data_classform/' + data_form + '/'
        n_folds_parent_folder = data_classform_folder_with_type + str(n_folds) + '_folds' + '/'
        Path(n_folds_parent_folder).mkdir(parents=True, exist_ok=True)
        fold_paths = []
        for i in range(n_folds):
            fold_paths.append(n_folds_parent_folder + 'fold_' + str(i) + '/')
            Path(fold_paths[i]).mkdir(parents=True, exist_ok=True)
            for project in projects:
                Path(fold_paths[i] + project).mkdir(parents=True, exist_ok=True)
        for data_type in data_types:
            for project in projects:
                for r, d, f in os.walk(data_classform_folder_with_type + data_type + '/' + project + '/'):
                    for file in f:
                        fold = rand_fold(n_folds)
                        copyfile(os.path.join(r, file), fold_paths[fold] + project + '/' + file)
