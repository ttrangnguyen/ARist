/**
 * 
 */
package flute.tokenizing.excode_data;

/**
 * @author ANH
 *
 */
public class ControlInfo {

	public static final int UNKNOWN = 0;
	public static final int FOR = 1;
	public static final int FOREACH = 2;
	public static final int DO = 3;
	public static final int WHILE = 4;
	public static final int IF = 5;
	public static final int ELSE = 6;
	public static final int CONDITIONAL = 7;
	public static final int SWITCH = 8;
	public static final int TRY = 9;
	public static final int CATCH = 10;
	public static final int FINALLY = 11;
	public static final int STATIC = 12;
	public static final int CASE = 13;
	public static final int SYNC = 14;

	public int type= UNKNOWN;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

	public ControlInfo(int type) {
		super();
		this.type = type;
	}
	

	public static String getNodeTypeName(int type){
		String tmp = "_UNKNWON_";
		switch (type) {
		case UNKNOWN:
			tmp = "_UNKNOWN_";
			break;
		case FOR:
			tmp = "_FOR_";
			break;
		case FOREACH:
			tmp = "_FOREACH_";
			break;
		case DO:
			tmp = "_DO_";
			break;
		case WHILE:
			tmp = "_WHILE_";
			break;
		case IF:
			tmp = "_IF_";
			break;
		case ELSE:
			tmp = "_ELSE_";
			break;
		case CONDITIONAL:
			tmp = "_CONDITIONAL_";
			break;
		case SWITCH:
			tmp = "_SWITCH_";
			break;
		case CASE:
			tmp = "_CASE_";
			break;
		case TRY:
			tmp = "_TRY_";
			break;
		case CATCH:
			tmp = "_CATCH_";
			break;
		case FINALLY:
			tmp = "_FINALLY_";
			break;
		case STATIC:
			tmp = "_STATIC_";
			break;
		case SYNC:
			tmp = "_SYNC_";
			break;
		default:
			break;
		}
		return tmp;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ControlInfo [type=");
		builder.append(getNodeTypeName(type));
		builder.append("]");
		return builder.toString();
	}

}
