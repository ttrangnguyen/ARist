/**
 * 
 */
package tokenizing.excode_data;

/**
 * @author ANH
 * This class represents a group of constants.
 * Each constant represents a token type.
 */
public class NodeSequenceConstant {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

	public static final short UNKNOWN = 0;
	public static final short CLASS = 1;
	public static final short ENUM = 2;
	public static final short METHOD = 3;
	public static final short CONSTRUCTOR = 4;
	public static final short CONTROL = 5;
	public static final short METHODACCESS = 6;
	public static final short CONSTRUCTORCALL = 7;
	public static final short FIELDACCESS = 8;
	public static final short BREAK = 9;
	public static final short CONTINUE = 10;
	public static final short TYPE = 11;
	public static final short VAR = 12;
	public static final short OPERATOR  = 13;
	public static final short UOPERATOR  = 14;
	public static final short LITERAL = 15;
	public static final short ASSIGN = 16;
	public static final short CAST = 17;
	public static final short FIELD = 18;
	
	public static final short IF = 100;
	public static final short ELSE = 101;
	public static final short COND = 102;
	public static final short WHILE = 103;
	public static final short FOR = 104;
	public static final short FOREACH = 105;
	public static final short DO = 106;
	public static final short SWITCH = 107;
	public static final short TRY = 108;
	public static final short CATCH = 109;
	public static final short FINALLY = 110;
	public static final short STATIC = 111;
	public static final short CASE = 112;
	public static final short SYNC = 113;
	public static final short EXPR = 116;
	public static final short CONSTR = 117;
	public static final short EXPL_CONSTR = 118;
	public static final short CASE_DEFAULT = 119;
	
	public static final short PARAM = 114;
	public static final short NPARAM = 115;
	
	public static final short START = 200;
	public static final short END = 201;
	public static final short NODE_PART = 203;
	
	public static final short OPBK = 300;
	public static final short CLBK = 301;
	public static final short SEPA = 302;
	public static final short RETURN = 303;
	public static final short THROW = 304;
	public static final short CASE_PART = 305;
	public static final short YIELD = 306;
	public static final short CONDITIONAL_EXPR = 400;
	
}
