grammar Excode;

//compilationUnit
//    :   typeDeclaration* EOF
//    ;

compilationUnit
    :   typeDeclaration* EOF
    ;

typeDeclaration
    :   (classDeclaration|enumDeclaration)
    ;

classDeclaration
    :   CLASS_START classBody CLASS_END
    ;

typeBound
    :   typeType (BOUND_AND typeType)*
    ;

enumDeclaration
    :   ENUM_START OPBLK enumConstants? enumBodyDeclarations? CLBLK ENUM_END
    ;

enumConstants
    :   enumConstant (COMMA enumConstant)*
    ;

enumConstant
	:	CLASS_START arguments? classBody? CLASS_END
	;

enumBodyDeclarations
    :   SEMI classBodyDeclaration*
    ;

classBody
    :   OPBLK classBodyDeclaration* CLBLK
    ;

classBodyDeclaration
    :   STATIC? block
    |   memberDeclaration
    ;

memberDeclaration
    :   methodDeclaration
    |   fieldDeclaration
    |   constructorDeclaration
    |   classDeclaration
    |   enumDeclaration
    ;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration
    :   METHOD LBRACE typeTypeOrVoid UNWRAPPED_COMMA identifier RBRACE methodDeclarator methodBody?
    ;

methodDeclarator
	:	OPEN_PART formalParameterList? CLOSE_PART
	;

methodBody
    :   block
    ;

typeTypeOrVoid
    :   typeType
    |   VOID
    ;

constructorDeclaration
    :   CONSTRUCTOR constructorDeclarator constructorBody
    ;

constructorDeclarator
	:	OPEN_PART formalParameterList? CLOSE_PART
	;

constructorBody
    :	OPBLK (STEXPLCONSTR explicitConstructorInvocation ENEXPLCONSTR)? blockStatement* CLBLK
    ;

explicitConstructorInvocation
	:	thisConstructorAccess arguments?
	|	superConstructorAccess arguments?
	|	variableDeclaratorId fieldAccess* superConstructorAccess arguments?
	|	primary superConstructorAccess arguments?
	;

thisConstructorAccess
    :   M_ACCESS LPAREN typeType UNWRAPPED_COMMA THIS UNWRAPPED_COMMA DECIMAL_LITERAL RPAREN
    ;

superConstructorAccess
    :   M_ACCESS LPAREN typeType UNWRAPPED_COMMA SUPER UNWRAPPED_COMMA DECIMAL_LITERAL RPAREN
    ;

fieldAccess
    :   F_ACCESS LPAREN typeType UNWRAPPED_COMMA identifier RPAREN
    ;

fieldDeclaration
    :   variableDeclarator
    ;

// see matching of [] comment in methodDeclaratorRest
// methodBody from Java8
// Java8

variableDeclarators
    :   variableDeclarator+
    ;

variableDeclarator
    :   wrappedType variableDeclaratorId (ASSIGN variableInitializer)?
    ;

variableDeclaratorId
	:	VAR LPAREN typeType UNWRAPPED_COMMA identifier (DOT THIS)? RPAREN
	;

variableInitializer
    :   arrayInitializer
    |   expression
    ;

arrayInitializer
    :   OPBLK (variableInitializer (COMMA variableInitializer)* COMMA? )? CLBLK
    ;

wrappedClassOrInterfaceType
    :   TYPE LPAREN classOrInterfaceType RPAREN
    ;

classOrInterfaceType
    :   identifier typeArguments? (DOT identifier typeArguments?)*
    ;

typeArgument
    :   typeType
    |   UNWRAPPED_QUESTION ((EXTENDS|SUPER) typeType)?
    ;

formalParameterList
    :   formalParameter (COMMA formalParameter)* (COMMA lastFormalParameter)?
    |   lastFormalParameter
    ;

formalParameter
    :   wrappedType variableDeclaratorId
    ;

lastFormalParameter
    :   wrappedTypeVarargs variableDeclaratorId
    ;

wrappedTypeVarargs
    :   TYPE LPAREN typeType ELLIPSIS RPAREN
    ;

wrappedQualifiedName
    :   TYPE LPAREN qualifiedName RPAREN
    ;

qualifiedName
    :   identifier (DOT identifier)*
    ;

wrappedLiteral
    :   LIT LPAREN literal RPAREN
    ;

literal
    :   primitiveType
    |   STRING
    |   NULL_LITERAL
    |   ZERO_LITERAL
    ;

elementValue
    :   expression
    |   elementValueArrayInitializer
    ;

elementValueArrayInitializer
    :   OPBLK (elementValue (COMMA elementValue)*)? COMMA? CLBLK
    ;


// STATEMENTS / BLOCKS

block
    :   OPBLK blockStatement* CLBLK
    ;

blockStatement
    :   STEXPR localVariableDeclaration ENEXPR
    |   statement
    |   localTypeDeclaration
    ;

localVariableDeclaration
    :   variableDeclarators
    ;

localTypeDeclaration
    :   classDeclaration
    ;

statement
    :   blockLabel=block
//    |   ASSERT expression (COLON expression)?
    |   STIF parExpression statement? ENIF (STELSE statement? ENELSE)?
    |   STFOR OPEN_PART forControl CLOSE_PART statement? ENFOR
    |   STFOREACH OPEN_PART enhancedForControl CLOSE_PART statement? ENFOREACH
    |   STWHILE parExpression statement? ENWHILE
    |   STDO statement? parExpression ENDO
    |   STTRY block ENTRY (catchClause+ finallyBlock?|finallyBlock)
    |   STTRY resourceSpecification block ENTRY catchClause* finallyBlock?
    |   STSWITCH parExpression OPBLK switchBlockStatementGroup* CLBLK ENSWITCH
    |   STSYNC parExpression block ENSYNC
    |   STRETURN expression? ENRETURN
    |   STTHROW expression ENTHROW
    |   STBREAK ENBREAK
    |   STCONTINUE ENCONTINUE
    |   STEXPR expression ENEXPR
//    |   identifierLabel=identifier COLON statement
    ;

catchClause
    :   STCATCH OPEN_PART catchType catchVariableDeclaratorId CLOSE_PART block ENCATCH
    ;

catchType
    :   wrappedQualifiedName (PIPE wrappedQualifiedName)*
    ;

catchVariableDeclaratorId
    :   VAR LPAREN typeType (UNWRAPPED_PIPE typeType)* UNWRAPPED_COMMA identifier (DOT THIS)? RPAREN
    ;

finallyBlock
    :   STFINALLY block ENFINALLY
    ;

resourceSpecification
	:	OPEN_PART resources SEMI? CLOSE_PART
    ;

resources
    :   resource (SEMI resource)*
    ;

resource
    :   wrappedClassOrInterfaceType variableDeclaratorId ASSIGN expression
	;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
	:	STCASE (constantExpression=expression|enumConstantName=identifier) CASE_PART blockStatement* ENCASE
	|   STDEFAULT CASE_PART blockStatement* ENDEFAULT
	;

forControl
    :   forInit? SEMI expression? SEMI forUpdate=expressionList?
    ;

forInit
    :   forVariableInit
    |   expressionList
    ;

forVariableInit
    :   wrappedType forVariableDeclarator (COMMA forVariableDeclarator)*
    ;

forVariableDeclarator
    :   variableDeclaratorId (ASSIGN variableInitializer)?
    ;

enhancedForControl
    :   wrappedType variableDeclaratorId COLON expression
    ;

// EXPRESSIONS

parExpression
    :   OPEN_PART expression CLOSE_PART
    ;

expressionList
    :   expression (COMMA expression)*
    ;

thisVariable
    :   VAR LPAREN typeType UNWRAPPED_COMMA (qualifiedName DOT)? THIS RPAREN
    ;

superVariable
    :   VAR LPAREN typeType UNWRAPPED_COMMA (qualifiedName DOT)? SUPER RPAREN
    ;

methodCallBody
    :   M_ACCESS LPAREN typeType UNWRAPPED_COMMA identifier UNWRAPPED_COMMA DECIMAL_LITERAL RPAREN
    ;

// temporary solution to fix antlr's bug
methodCall
    :   methodCallBody (arguments|arguments)
    ;

expression
    :   primary
    |   expression
        (   fieldAccess
        |   methodCall
        |   thisVariable
//        |   NEW nonWildcardTypeArguments? innerCreator
        |   superVariable superSuffix
        |   explicitGenericInvocation
        )
    |   expression OPEN_PART expression CLOSE_PART
    |   methodCall
    |   creator
    |   CAST LPAREN typeBound RPAREN expression
    |   expression postfix=(POS_INCREMENT|POS_DECREMENT)
    |   prefix=(POSITIVE|NEGATIVE|PRE_INCREMENT|PRE_DECREMENT) expression
    |   prefix=(COMPLEMENT|NOT) expression
    |   expression bop=(TIMES|DIVIDE|REMAINDER) expression
    |   expression bop=(PLUS|MINUS) expression
    |   expression (L_SHIFT|R_UNSIGNED_SHIFT|R_SIGNED_SHIFT) expression
    |   expression bop=(LESS_EQUALS|GREATER_EQUALS|GREATER|LESS) expression
    |   expression bop=INSTANCEOF wrappedType
    |   expression bop=(EQUALS|NOT_EQUALS) expression
    |   expression bop=BIN_AND expression
    |   expression bop=XOR expression
    |   expression bop=BIN_OR expression
    |   expression bop=AND expression
    |   expression bop=OR expression
    |   <assoc=right> CEXP expression bop=QUESTION expression COLON expression
    |   <assoc=right> expression
        bop=(ASSIGN
         |   PLUS_ASSIGN
         |   MINUS_ASSIGN
         |   MUL_ASSIGN
         |   DIVIDE_ASSIGN
         |   AND_ASSIGN
         |   OR_ASSIGN
         |   XOR_ASSIGN
         |   R_SIGNED_SHIFT_ASSIGN
         |   R_UNSIGNED_SHIFT_ASSIGN
         |   L_SHIFT_ASSIGN
         |   REM_ASSIGN)
        expression
    ;

primary
    :   OPEN_PART expression CLOSE_PART
    |   VAR LPAREN typeType UNWRAPPED_COMMA (typeType DOT)? THIS RPAREN
    |   VAR LPAREN typeType UNWRAPPED_COMMA (typeType DOT)? SUPER RPAREN
    |   wrappedLiteral
    |   variableDeclaratorId
	|   VAR LPAREN typeTypeOrVoid (DOT CLASS)? UNWRAPPED_COMMA typeTypeOrVoid DOT CLASS RPAREN
//    |   nonWildcardTypeArguments (explicitGenericInvocationSuffix|THIS arguments)
    ;

creator
    :   C_CALL LPAREN createdName UNWRAPPED_COMMA typeType RPAREN classCreatorRest
    ;

createdName
    :   identifier typeArgumentsOrDiamond? (DOT identifier typeArgumentsOrDiamond?)*
    |   primitiveType
    ;

//arrayCreatorRest
//    :   LBRACK (RBRACK (LBRACK RBRACK)* arrayInitializer | expression RBRACK (LBRACK expression RBRACK)* (LBRACK RBRACK)*)
//    ;

classCreatorRest
    :   arguments
    |   arguments CLASS_START classBody CLASS_END
    ;

explicitGenericInvocation
    :   nonWildcardTypeArguments explicitGenericInvocationSuffix
    ;

typeArgumentsOrDiamond
    :   TYPE_ARGS_OPEN TYPE_ARGS_CLOSE
    |   typeArguments
    ;

nonWildcardTypeArguments
    :   TYPE_ARGS_OPEN typeList TYPE_ARGS_CLOSE
    ;

typeList
    :   typeType (COMMA typeType)*
    ;

wrappedType
    :   TYPE LPAREN typeType RPAREN
    ;

typeType
    :   (classOrInterfaceType|primitiveType)
    |   (classOrInterfaceType|primitiveType) LBRACK (RBRACK LBRACK)* RBRACK
    |   UNKNOWN
    ;

primitiveType
    :   BOOLEAN
    |   CHAR
    |   BYTE
    |   SHORT
    |   INT
    |   LONG
    |   FLOAT
    |   DOUBLE
    ;

typeArguments
    :   TYPE_ARGS_OPEN typeArgument (UNWRAPPED_COMMA typeArgument)* TYPE_ARGS_CLOSE
    ;

superSuffix
    :   arguments
    |   DOT identifier arguments?
    ;

explicitGenericInvocationSuffix
    :   SUPER superSuffix
    |   identifier arguments
    ;

arguments
    :   OPEN_PART expressionList? CLOSE_PART
    ;

identifier
    :   IDENTIFIER
    |   STRING
    |   STATIC
    |   TYPE
    |   METHOD
    |   VAR
    |   LIT
    |   M_ACCESS
    |   F_ACCESS
    |   C_CALL
    |   OPEN_PART
    |   CLOSE_PART
    |   UNKNOWN
    |   CAST
    |   CONSTRUCTOR
    |   CASE_PART
    |   CEXP
    |   OPBLK
    |   CLBLK
    |   ZERO_LITERAL
    ;

BOOLEAN : 'boolean';
BYTE : 'byte';
CHAR : 'char';
CLASS : 'class';
DOUBLE : 'double';
EXTENDS : 'extends';
FLOAT : 'float';
INT : 'int';
LONG : 'long';
SHORT : 'short';
STATIC : 'STATIC';
SUPER : 'super';
THIS : 'this';
VOID : 'void';

// excode language keywords
TYPE : 'TYPE';
VAR : 'VAR';
LIT : 'LIT';
STRING : 'String';
M_ACCESS : 'M_ACCESS';
F_ACCESS : 'F_ACCESS';
C_CALL : 'C_CALL';
OPEN_PART : 'OPEN_PART';
CLOSE_PART : 'CLOSE_PART';
UNKNOWN : '<unk>';

// statements"
STFOR : 'STSTM{FOR}';
ENFOR : 'ENSTM{FOR}';
STFOREACH : 'STSTM{FOREACH}';
ENFOREACH : 'ENSTM{FOREACH}';
STIF : 'STSTM{IF}';
ENIF : 'ENSTM{IF}';
STWHILE : 'STSTM{WHILE}';
ENWHILE : 'ENSTM{WHILE}';
STDO : 'STSTM{DO}';
ENDO : 'ENSTM{DO}';
STSYNC : 'STSTM{SYNC}';
ENSYNC : 'ENSTM{SYNC}';
STEXPR : 'STSTM{EXPR}';
ENEXPR : 'ENSTM{EXPR}';
STCONTINUE : 'STSTM{CONTINUE}';
ENCONTINUE : 'ENSTM{CONTINUE}';
STBREAK : 'STSTM{BREAK}';
ENBREAK : 'ENSTM{BREAK}';
STSWITCH : 'STSTM{SWITCH}';
ENSWITCH : 'ENSTM{SWITCH}';
STCASE : 'STSTM{CASE}';
ENCASE : 'ENSTM{CASE}';
STDEFAULT : 'STSTM{CASE_DEFAULT}';
ENDEFAULT : 'ENSTM{CASE_DEFAULT}';
STRETURN : 'STSTM{RETURN}';
ENRETURN : 'ENSTM{RETURN}';
STTHROW : 'STSTM{THROW}';
ENTHROW : 'ENSTM{THROW}';
STTRY : 'STSTM{TRY}';
ENTRY : 'ENSTM{TRY}';
STCATCH : 'STSTM{CATCH}';
ENCATCH : 'ENSTM{CATCH}';
STEXPLCONSTR : 'STSTM{EXPL_CONSTR}';
ENEXPLCONSTR : 'ENSTM{EXPL_CONSTR}';
STELSE : 'STSTM{ELSE}';
ENELSE : 'ENSTM{ELSE}';
STFINALLY : 'STSTM{FINALLY}';
ENFINALLY : 'ENSTM{FINALLY}';

CAST : 'CAST';

// class, enum, interface
CLASS_START : 'CLASS{START}';
CLASS_END : 'CLASS{END}';
ENUM_START : 'ENUM{START}';
ENUM_END : 'ENUM{END}';
METHOD : 'METHOD';
CONSTRUCTOR : 'CONSTRUCTOR';
CASE_PART : 'CASE_PART';
CEXP : 'CEXP';

// §3.11 Separators

LPAREN : '(';
RPAREN : ')';
LBRACE : '{';
RBRACE : '}';
OPBLK : 'OPBLK';
CLBLK : 'CLBLK';
LBRACK : '[';
RBRACK : ']';
COLON : 'SEPA(:)';
SEMI : 'SEPA(;)';
COMMA : 'SEPA(,)';
UNWRAPPED_COMMA : ',';
UNWRAPPED_PIPE : '|';
PIPE : 'SEPA(|)';
DOT : '.';
ELLIPSIS : '...';
BOUND_AND : '&';
TYPE_ARGS_OPEN : '<';
TYPE_ARGS_CLOSE : '>';

QUESTION : 'SEPA(?)';
UNWRAPPED_QUESTION : '?';


// §3.12 Operators

// assignment operators
ASSIGN : 'ASSIGN(ASSIGN)';
PLUS_ASSIGN : 'ASSIGN(PLUS)';
MINUS_ASSIGN : 'ASSIGN(MINUS)';
MUL_ASSIGN : 'ASSIGN(MULTIPLY)';
DIVIDE_ASSIGN : 'ASSIGN(DIVIDE)';
AND_ASSIGN : 'ASSIGN(BINARY_AND)';
OR_ASSIGN : 'ASSIGN(BINARY_OR)';
XOR_ASSIGN : 'ASSIGN(XOR)';
REM_ASSIGN : 'ASSIGN(REMAINDER)';
L_SHIFT_ASSIGN : 'ASSIGN(LEFT_SHIFT)';
R_SIGNED_SHIFT_ASSIGN : 'ASSIGN(SIGNED_RIGHT_SHIFT)';
R_UNSIGNED_SHIFT_ASSIGN : 'ASSIGN(UNSIGNED_RIGHT_SHIFT)';

// unary operators
POSITIVE : 'UOP(PLUS)';
NEGATIVE : 'UOP(MINUS)';
PRE_INCREMENT : 'UOP(PREFIX_INCREMENT)';
PRE_DECREMENT : 'UOP(PREFIX_DECREMENT)';
NOT : 'UOP(LOGICAL_COMPLEMENT)';
COMPLEMENT : 'UOP(BITWISE_COMPLEMENT)';
POS_INCREMENT: 'UOP(POSTFIX_INCREMENT)';
POS_DECREMENT: 'UOP(POSTFIX_DECREMENT)';

// binary operators
OR : 'OP(OR)';
AND : 'OP(AND)';
BIN_OR : 'OP(BINARY_OR)';
BIN_AND : 'OP(BINARY_AND)';
XOR : 'OP(XOR)';
EQUALS : 'OP(EQUALS)';
NOT_EQUALS : 'OP(NOT_EQUALS)';
LESS : 'OP(LESS)';
GREATER : 'OP(GREATER)';
LESS_EQUALS : 'OP(LESS_EQUALS)';
GREATER_EQUALS : 'OP(GREATER_EQUALS)';
L_SHIFT : 'OP(LEFT_SHIFT)';
R_SIGNED_SHIFT : 'OP(SIGNED_RIGHT_SHIFT)';
R_UNSIGNED_SHIFT : 'OP(UNSIGNED_RIGHT_SHIFT)';
PLUS : 'OP(PLUS)';
MINUS : 'OP(MINUS)';
TIMES : 'OP(MULTIPLY)';
DIVIDE : 'OP(DIVIDE)';
REMAINDER : 'OP(REMAINDER)';
INSTANCEOF : 'OP(INSTANCEOF)';

// Literals

DECIMAL_LITERAL:    ('0'|[1-9] (Digits?|'_'+ Digits)) [lL]?;
HEX_LITERAL:        '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL:        '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL:     '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:      (Digits '.' Digits?|'.' Digits) ExponentPart? [fFdD]?
             |      Digits (ExponentPart [fFdD]?|[fFdD])
             ;

HEX_FLOAT_LITERAL:  '0' [xX] (HexDigits '.'?|HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL:       'true'
            |       'false'
            ;

CHAR_LITERAL:       '\'' (~['\\\r\n]|EscapeSequence) '\'';

STRING_LITERAL:     '"' (~["\\\r\n]|EscapeSequence)* '"';
NULL_LITERAL:       'null';
ZERO_LITERAL:       'zero';

// Whitespace and comments
WS:                 [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT:            '/*' .*? '*/'    -> channel(HIDDEN);
LINE_COMMENT:       '//' ~[\r\n]*    -> channel(HIDDEN);

// Identifiers

IDENTIFIER:         Letter LetterOrDigit*;

// Fragment rules

fragment ExponentPart
    :   [eE] [+-]? Digits
    ;

fragment EscapeSequence
    :   '\\' [btnfr"'\\]
    |   '\\' ([0-3]? [0-7])? [0-7]
    |   '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigits
    :   HexDigit ((HexDigit|'_')* HexDigit)?
    ;

fragment HexDigit
    :   [0-9a-fA-F]
    ;

fragment Digits
    :   [0-9] ([0-9_]* [0-9])?
    ;

fragment LetterOrDigit
    :   Letter
    |   [0-9]
    ;

fragment Letter
    :   [a-zA-Z$_] // these are the "java letters" below 0x7F
    |   ~[\u0000-\u007F\uD800-\uDBFF] // covers all characters above 0x7F which are not a surrogate
    |   [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
    ;
