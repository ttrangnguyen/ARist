/**
 * 
 */
package flute.tokenizing.visitors;

import java.io.File;
import java.util.*;

import com.github.javaparser.Position;
import flute.analysis.config.Config;
import flute.utils.file_processing.DirProcessor;
import flute.tokenizing.exe.GetDirStructureCrossProject;
import flute.tokenizing.parsing.JavaFileParser;

import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import flute.tokenizing.excode_data.ControlInfo;
import flute.tokenizing.excode_data.FileInfo;
import flute.tokenizing.excode_data.MethodInfo;
import flute.tokenizing.excode_data.MethodInvocInfo;
import flute.tokenizing.excode_data.NodeInfo;
import flute.tokenizing.excode_data.NodeSequenceConstant;
import flute.tokenizing.excode_data.NodeSequenceInfo;
import flute.tokenizing.excode_data.SystemTableCrossProject;
import flute.tokenizing.excode_data.TypeInfo;
import flute.utils.logging.Logger;

/**
 * @author ANH
 * Each java file denotes a compilation unit represented by {@link CompilationUnit}.
 * This class is a visitor which traverses the compilation unit and
 * extracts code tokens and other information from it.
 * A token is annotated with its flute.data type and semantic role if available.
 * Code tokens represented by {@link NodeSequenceInfo} are stored in {@link MetricsVisitor#nodeSequenceList}.
 * Other information about the file are stored in {@link MetricsVisitor#fileInfo}.
 * @see MetricsVisitor#visit(CompilationUnit, Object)
 */
public class MetricsVisitor extends VoidVisitorAdapter<Object> {
	private static final boolean CONFIG_RAW = false;
    public  static int ID_SUFFIX = 452961;

	public  static Map<String, String> DIC = new HashMap<>();
	public  static Map<String, String> METHOD_FILE = new HashMap<>();

	private FileInfo fileInfo = null;
	private TypeInfo curTypeInfo = null;
	private MethodInfo curMethodInfo = null;
	// TreeMap<String, TypeInfo> typeInfoMap = new TreeMap<String, TypeInfo>();
	private Stack<MethodInfo> methodInfoStack = new Stack<MethodInfo>();
	private Stack<TypeInfo> typeStack = new Stack<TypeInfo>();

	@SuppressWarnings("unused")
    private NodeInfo curParentNode = null;
	private Stack<NodeInfo> parentNodeStack = new Stack<NodeInfo>();

	@SuppressWarnings("unused")
    private NodeInfo curNode = null;
	private Stack<ArrayList<NodeInfo>> previousControlFlowNodeStack = new Stack<ArrayList<NodeInfo>>();

	@SuppressWarnings("unused")
    private NodeSequenceInfo curNodeSequenceInfo = null;
	private Stack<NodeSequenceInfo> nodeSequenceStack = new Stack<NodeSequenceInfo>();
	public ArrayList<NodeSequenceInfo> nodeSequenceList = new ArrayList<NodeSequenceInfo>();

	private long curID = 0;

	public boolean isParam = true;

	public synchronized static void resetDIC() {
		DIC = new HashMap<>();
		// METHOD_FILE = new HashMap<>();
	}

	/**
	 * Attaches the file information and its {@link FileInfo#nodeSequenceList} to the visitor.
	 * Initializes class OldNodeSequenceVisiting.
	 * @param fileInfo The file information.
	 * @see    OldNodeSequenceVisitingProcessing#init()
	 */
	public void init(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
		this.nodeSequenceList = fileInfo.nodeSequenceList;
		OldNodeSequenceVisitingProcessing.init();
	}

	public void resetAll() {
		fileInfo = null;
		curTypeInfo = null;
		curMethodInfo = null;
		// TreeMap<String, TypeInfo> typeInfoMap = new TreeMap<String, TypeInfo>();
		methodInfoStack = new Stack<MethodInfo>();
		typeStack = new Stack<TypeInfo>();

		curParentNode = null;
		parentNodeStack = new Stack<NodeInfo>();

		curNode = null;
		previousControlFlowNodeStack = new Stack<ArrayList<NodeInfo>>();

		curNodeSequenceInfo = null;
		nodeSequenceStack = new Stack<NodeSequenceInfo>();
	}
	
	// Eg: a """in""" int x = a + 3;
    @Override
    public void visit(NameExpr n, Object arg) {
        //Logger.log("NameExpr: " + n + "\t" + n.getName());

        Node parentNode = n.getParentNode().orElse(null);
        //Logger.log(parentNode.getClass());
        //if (parentNode instanceof Expression || parentNode instanceof ForEachStmt || parentNode instanceof IfStmt
        //        || parentNode instanceof ReturnStmt || parentNode instanceof SynchronizedStmt
        //        || parentNode instanceof ExplicitConstructorInvocationStmt
        //        || parentNode instanceof ThrowStmt
        //        || parentNode instanceof SwitchStmt || parentNode instanceof SwitchEntry
        //        || parentNode instanceof ArrayAccessExpr || parentNode instanceof ArrayCreationLevel
        //        || parentNode instanceof VariableDeclarator || parentNode instanceof FieldDeclaration) {
            // if (parentNode != null && !(parentNode instanceof AnnotationExpr) &&
            // parentNode.getParentNode() != null) {
            {
                // a ---> VAR(int,a)
                // Logger.logDebug("NameExpr: " + n + "\t" + n.getName() + "\t" + parentNode);
                NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack,
                        previousControlFlowNodeStack, curID, parentNode);
                String varName = n.getName().asString();
                OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, varName, nodeSequenceStack, curMethodInfo,
                        curTypeInfo, nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
                curID++;
        //    }
        }
        // checkMethodCallExpr(n);
    }
	
	// A name that consists of a single identifier.
    @Override
    public void visit(SimpleName n, Object arg) {
    }
    
    // A name that may consist of multiple identifiers
    @Override
    public void visit(Name n, Object arg) {
    }
	
    // Ignores Module Declaration
	@Override
    public void visit(CompilationUnit n, Object arg) {
	    if (n == null) return;
	    if (arg == null) arg = new HashSet<String>();
	    if (n.getPackageDeclaration().isPresent()) visit(n.getPackageDeclaration().get(), arg);
	    for (ImportDeclaration p: n.getImports()) visit(p, arg);
	    for (TypeDeclaration<?> p: n.getTypes()) p.accept(this, arg);
    }
	
	// Eg: package com.github.javaparser.ast;
    @Override
    public void visit(PackageDeclaration n, Object arg) {
        fileInfo.packageDec = n.getName().toString();
    }
	
	// Eg: import static com.github.javaparser.JavaParser.*;
    @Override
    public void visit(ImportDeclaration n, Object arg) {
        // Logger.log("importdec: " + n.getName());
        fileInfo.importList.add(n.getName().toString().intern());

    }
	
	// Eg: class X { ... }
    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        //fileInfo.numStatements++;
        
        if (typeStack.size() > 0) {
            curTypeInfo = typeStack.peek();
        }
        if (methodInfoStack.size() > 0) {
            curMethodInfo = methodInfoStack.peek();
        }

        // Logger.logDebug("ClassOrInterfaceDeclaration: " + n.getName() + "\t" +
        // n.getParentNode().getClass() + "\t" + fileInfo.filePath);

        TypeInfo typeInfo = new TypeInfo();
        typeInfo.typeName = n.getName().asString();
        typeInfo.packageDec = fileInfo.packageDec;

        // n.getModifiers();
        // file type
        fileInfo.typeInfoList.add(typeInfo);

        // Add extend list
        List<ClassOrInterfaceType> extendTypes = n.getExtendedTypes();
        if (extendTypes != null) {
            if (extendTypes.size() > 0) {
                typeInfo.extendTypeStrList = new ArrayList<String>();
                for (ClassOrInterfaceType extendType : extendTypes) {
                    String fullname = extendType.asString();
                    typeInfo.extendTypeStrList.add(fullname);
                }
            }
        }

        // Add implement list
        List<ClassOrInterfaceType> implementTypes = n.getImplementedTypes();
        if (implementTypes != null) {
            if (implementTypes.size() > 0) {
                typeInfo.implementTypeStrList = new ArrayList<String>();
                for (ClassOrInterfaceType implementType : implementTypes) {
                    String fullname = implementType.asString();
                    typeInfo.implementTypeStrList.add(fullname);
                }
            }
        }

        for (Modifier mod : n.getModifiers()) {
            if (mod.getKeyword() == Keyword.PUBLIC) {
                typeInfo.accessModType = "public";
            } else if (mod.getKeyword() == Keyword.PRIVATE) {
                typeInfo.accessModType = "private";
            } else if (mod.getKeyword() == Keyword.PROTECTED) {
                typeInfo.accessModType = "protected";
            }
        }

        // Logger.log("ClassOrInterfaceDeclaration type: " + typeInfo.accessModType );

        typeInfo.fileInfo = fileInfo;

        Node ascendant = getAscendantTypeMethod(n);
        if ((ascendant instanceof ClassOrInterfaceDeclaration) || (ascendant instanceof EnumDeclaration)
                || (ascendant instanceof AnnotationDeclaration)) {
            typeInfo.parentInfo = curTypeInfo;
        } else if ((ascendant instanceof MethodDeclaration) || (ascendant instanceof ConstructorDeclaration)) {
            typeInfo.parentInfo = curMethodInfo;
        }

        curTypeInfo = typeInfo;

        // typeInfoMap.put(typeInfo.typeName, typeInfo);
        typeStack.push(typeInfo);
        
        // Add this variable for current class
        addVariableToScope(n, typeInfo.typeName.intern(), "this");
        addVariableToScope(n, typeInfo.typeName.intern(), typeInfo.typeName.intern() + ".this");

        // Add super variable for superclass
        if (typeInfo.extendTypeStrList != null) {
            addVariableToScope(n, typeInfo.extendTypeStrList.get(0).intern(), "super");
            addVariableToScope(n, typeInfo.extendTypeStrList.get(0).intern(), typeInfo.typeName.intern() + ".super");
        }

        // class X ---> CLASS{START,X}
        NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addClassNode(typeInfo.typeName.intern(),
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getBegin(), null);
        nodeSequenceInfo.oriNode = n;
        
        // {...} --->
        visitClassBody(n.getMembers(), arg);
        
        // ---> CLASS{END,X}
        OldNodeSequenceVisitingProcessing.addEndClassNode(typeInfo.typeName.intern(), nodeSequenceInfo.nodeSeqID,
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getEnd());
        
        curMethodInfo = null;
        typeStack.pop();
    }
    
    // Eg: enum X { A, B; ... }
    @Override
    public void visit(EnumDeclaration n, Object arg) {
        //fileInfo.numStatements++;

        if (typeStack.size() > 0) {
            curTypeInfo = typeStack.peek();
        }
        if (methodInfoStack.size() > 0) {
            curMethodInfo = methodInfoStack.peek();
        }
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.typeName = n.getName().asString();
        typeInfo.packageDec = fileInfo.packageDec;

        // n.getModifiers();
        // file type
        fileInfo.typeInfoList.add(typeInfo);

        // Add implement list
        List<ClassOrInterfaceType> implementTypes = n.getImplementedTypes();
        if (implementTypes != null) {
            if (implementTypes.size() > 0) {
                typeInfo.implementTypeStrList = new ArrayList<String>();
                for (ClassOrInterfaceType implementType : implementTypes) {
                    String fullname = implementType.asString();
                    typeInfo.implementTypeStrList.add(fullname);
                }
            }
        }

        for (Modifier mod : n.getModifiers()) {
            if (mod.getKeyword() == Keyword.PUBLIC) {
                typeInfo.accessModType = "public";
            } else if (mod.getKeyword() == Keyword.PRIVATE) {
                typeInfo.accessModType = "private";
            } else if (mod.getKeyword() == Keyword.PROTECTED) {
                typeInfo.accessModType = "protected";
            }
        }

        // Logger.log("ClassOrInterfaceDeclaration type: " + typeInfo.accessModType );

        typeInfo.fileInfo = fileInfo;

        Node ascendant = getAscendantTypeMethod(n);
        if ((ascendant instanceof ClassOrInterfaceDeclaration) || (ascendant instanceof EnumDeclaration)
                || (ascendant instanceof AnnotationDeclaration)) {
            typeInfo.parentInfo = curTypeInfo;
        } else if ((ascendant instanceof MethodDeclaration) || (ascendant instanceof ConstructorDeclaration)) {
            typeInfo.parentInfo = curMethodInfo;

        }

        curTypeInfo = typeInfo;

        // typeInfoMap.put(typeInfo.typeName, typeInfo);
        typeStack.push(typeInfo);
        
        // Add this variable for current class
        addVariableToScope(n, typeInfo.typeName.intern(), "this");
        addVariableToScope(n, typeInfo.typeName.intern(), typeInfo.typeName.intern() + ".this");
        
        for (EnumConstantDeclaration entry : n.getEntries()) {
            addVariableToScope(n, typeInfo.typeName.intern(), entry.getName().asString());
        }

        // enum X ---> ENUM{START,X}
        NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addEnumNode(typeInfo.typeName.intern(),
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getBegin(), null);
        nodeSequenceInfo.oriNode = n;
        
        // { ---> OPBLK
        OldNodeSequenceVisitingProcessing.addOPBLKNode(nodeSequenceList);
                //.setPosition(getPositionOfStringFrom(n.getBegin(), getStringWithoutComment(n), "{"));
        
        if (n.getEntries().isNonEmpty()) {
            for (int i = 0; i < n.getEntries().size() - 1; ++i) {
                // A --->
                visit(n.getEntry(i), arg);
                
                // , ---> SEPA(,)
                OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ',')
                        .setPosition(getPositionFrom(n.getEntry(i).getEnd(), 1));
            }
            // B --->
            visit(n.getEntry(n.getEntries().size() - 1), arg);
        }
        
        if (n.getMembers().isNonEmpty()) {
            // ; ---> SEPA(;)
            NodeSequenceInfo sepa = OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ';');
            if (n.getEntries().isNonEmpty()) {
                sepa.setPosition(getPositionFrom(n.getEntry(n.getEntries().size() - 1).getEnd(), 1));
            }

            
            // ... --->
            n.getMembers().forEach(p -> p.accept(this, arg));
        }
        
        // } ---> CLBLK
        OldNodeSequenceVisitingProcessing.addCLBLKNode(nodeSequenceList).setPosition(n.getEnd());
        
        // ---> ENUM{END,X}
        OldNodeSequenceVisitingProcessing.addEndEnumNode(typeInfo.typeName.intern(), nodeSequenceInfo.nodeSeqID,
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getEnd());
        
        curMethodInfo = null;
        typeStack.pop();
    }
    
    // Eg: @interface X { ... }
    @Override
    public void visit(AnnotationDeclaration n, Object arg) {
        //fileInfo.numStatements++;
        
        if (typeStack.size() > 0) {
            curTypeInfo = typeStack.peek();
        }
        if (methodInfoStack.size() > 0) {
            curMethodInfo = methodInfoStack.peek();
        }
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.typeName = n.getName().asString();
        typeInfo.packageDec = fileInfo.packageDec;

        fileInfo.typeInfoList.add(typeInfo);

        for (Modifier mod : n.getModifiers()) {
            if (mod.getKeyword() == Keyword.PUBLIC) {
                typeInfo.accessModType = "public";
            } else if (mod.getKeyword() == Keyword.PRIVATE) {
                typeInfo.accessModType = "private";
            } else if (mod.getKeyword() == Keyword.PROTECTED) {
                typeInfo.accessModType = "protected";
            }
        }

        typeInfo.fileInfo = fileInfo;

        Node ascendant = getAscendantTypeMethod(n);
        if ((ascendant instanceof ClassOrInterfaceDeclaration) || (ascendant instanceof EnumDeclaration)
                || (ascendant instanceof AnnotationDeclaration)) {
            typeInfo.parentInfo = curTypeInfo;
        } else if ((ascendant instanceof MethodDeclaration) || (ascendant instanceof ConstructorDeclaration)) {
            typeInfo.parentInfo = curMethodInfo;

        }

        curTypeInfo = typeInfo;

        typeStack.push(typeInfo);

        // @interface X ---> CLASS{START,X}
        NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addClassNode(typeInfo.typeName.intern(),
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getBegin(), null);
        nodeSequenceInfo.oriNode = n;
        
        // {...} --->
        visitClassBody(n.getMembers(), arg);
        
        // ---> CLASS{END,X}
        OldNodeSequenceVisitingProcessing.addEndClassNode(typeInfo.typeName.intern(), nodeSequenceInfo.nodeSeqID,
                nodeSequenceStack, typeInfo, nodeSequenceList).setPosition(n.getEnd());
        
        curMethodInfo = null;
        typeStack.pop();
    }
    
    // Eg: short
    @Override
    public void visit(PrimitiveType n, Object arg) {
        // short ---> TYPE(short)
        // Logger.log("PrimitiveType: " + n);
        String type = n.asString();
        
        // For varargs
        if ((arg instanceof Set) && ((Set<?>)arg).contains("varargs")) type += "...";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addTypeNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addTypeNode(nodeInfo, type, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
        curID++;
    }
    
    // Eg: int[][]
    @Override
    public void visit(ArrayType n, Object arg) {
        String type = n.toString();
        
        // For varargs
        if ((arg instanceof Set) && ((Set<?>)arg).contains("varargs")) type += "...";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addTypeNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addTypeNode(nodeInfo, type, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
        curID++;
    }
    
    // Eg: HashMap<String, String>
    @Override
    public void visit(ClassOrInterfaceType n, Object arg) {
        // Node parentNode = n.getParentNode();
        // Logger.log("ClassOrInterfaceType: " +n +"\tparent: " + parentNode +
        // "\tparentType:" + parentNode.getClass());
        // Logger.log("ClassOrInterfaceType: " +n +"\t" + n.getBeginLine() + "\t" +
        // n.getEndLine());
        // Logger.logDebug("ClassOrInterfaceDeclaration: " + n.getName() + "\t" +
        // n.getParentNode().getClass() + "\t" + fileInfo.filePath);
        
        String type = n.getNameAsString();
        
        // For varargs
        if ((arg instanceof Set) && ((Set<?>)arg).contains("varargs")) type += "...";
        
        NodeInfo typeNodeInfo = NodeVisitProcessing.addTypeNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addTypeNode(typeNodeInfo, type, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
        curID++;

        //super.visit(n, arg);
    }
    
    // Eg: var a = new ArrayList<String>();
    @Override
    public void visit(VarType n, Object arg) {
        // var ---> TYPE(var)
        String type = n.toString();
        
        // For varargs
        if ((arg instanceof Set) && ((Set<?>)arg).contains("varargs")) type += "...";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addTypeNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addTypeNode(nodeInfo, type, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
        curID++;
    }
    
    @Override
    public void visit(WildcardType n, Object arg) {
        // Logger.log("WildcardType:" + n);
        fileInfo.numWildcards++;
        String literalType = "wildcard";
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        
        // ? ---> LIT(wildcard)
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam).setPosition(n.getBegin(), n.getEnd());
        curID++;
    }
    
    // TODO: may handle this in the future 
    // Eg: @Brain T extends B & A & @Tripe C
    @Override
    public void visit(TypeParameter n, Object arg) {
        // Logger.log("TypeParameter:" + n);
        //super.visit(n, arg);
    }
    
    // TODO: may handle this in the future
    // Eg: Serializable & Cloneable """in""" void foo((Serializable & Cloneable)myObject);
    @Override
    public void visit(IntersectionType n, Object arg) {
        //super.visit(n, arg);
    }
    
    // void
    @Override
    public void visit(VoidType n, Object arg) {
        // Logger.log("VoidType:" + n);

        // String literalType = "void";
        // NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo,
        // parentNodeStack, previousControlFlowNodeStack, curID, n);
        // NodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType,
        // nodeSequenceStack, curMethodInfo, curTypeInfo, nodeSequenceList);
        // curID++;
    }
    
    // Eg: @Override
    @Override
    public void visit(MarkerAnnotationExpr n, Object arg) {
        //Logger.log("MarkerAnnotationExpr: " + n);
    }
    
    // Eg: @Mapping(a=5, d=10)
    @Override
    public void visit(NormalAnnotationExpr n, Object arg) {
        //Logger.log("NormalAnnotationExpr: " + n);
    }

    // Eg: @Count(15)
    @Override
    // @SuppressWarnings("synthetic-access")
    public void visit(SingleMemberAnnotationExpr n, Object arg) {
        //Logger.log("SingleMemberAnnotationExpr: " + n);
    }
    
    // Eg: {...}
    public void visitClassBody(NodeList<BodyDeclaration<?> > n, Object arg) {
	    Node parentNode = n.getParentNode().get();

        // { ---> OPBLK
        OldNodeSequenceVisitingProcessing.addOPBLKNode(nodeSequenceList);
                //.setPosition(getPositionOfStringFrom(parentNode.getBegin(), getStringWithoutComment(parentNode), "{"));
        
        // ... --->
        for (BodyDeclaration<?> p: n) p.accept(this, arg);
        
        // } ---> CLBLK
        OldNodeSequenceVisitingProcessing.addCLBLKNode(nodeSequenceList)
                .setPosition(parentNode.getEnd());
    }
    
    // Eg: private static int a=15*15; """in""" class X { private static int a=15*15; }
    @Override
    public void visit(FieldDeclaration n, Object arg) {
        //fileInfo.numStatements++;
        
        for (VariableDeclarator p: n.getVariables()) {
            // ---> FIELD_DECLARE
            //OldNodeSequenceVisitingProcessing.addFieldDecNode(nodeSequenceList);
            
            // private static int a=15*15; --->
            visit(p, arg);
        }
    }
    
    // Eg: public int add(int a, int b) {return a + b;}
    @Override
    public void visit(MethodDeclaration n, Object arg) {
        if (n.getBody() == null || (n.getBody().isPresent() && n.getBody().get().toString().length() < 5)) {
            // super.visit(n, arg);
        } else {

            MetricsVisitor.ID_SUFFIX++;
            curTypeInfo = typeStack.peek();
            fileInfo.numMethodDecs++;

            // Node tmp = n.getParentNode();
            // while (!(tmp instanceof ClassOrInterfaceDeclaration)){
            // tmp = n.getParentNode();
            // }

            String methodName = n.getName().asString();
            ArrayList<String> paramsList = null;
            if (n.getParameters() != null) {
                if (n.getParameters().size() > 0) {
                    paramsList = new ArrayList<String>();
                    for (Parameter p : n.getParameters()) {
                        paramsList.add(p.getType().asString() + (p.isVarArgs()? "[]": ""));
                    }
                }
            }

            MethodInfo methodInfo = new MethodInfo(methodName, paramsList, curTypeInfo, fileInfo);
            curMethodInfo = methodInfo;

            if (curMethodInfo != null) {
                methodInfoStack.push(curMethodInfo);
            }

            // Logger.log("curMethodInfo: " + curMethodInfo.getFullMethodSignature() +
            // "\t" + methodInfoStack.peek().getFullMethodSignature());

            if ((curTypeInfo.methodDecMap != null) && (curTypeInfo.methodDecMap.containsKey(methodName))) {
                curTypeInfo.methodDecMap.get(methodName).add(methodInfo);
            } else {
                ArrayList<MethodInfo> methodList = new ArrayList<MethodInfo>();
                methodList.add(methodInfo);
                if (curTypeInfo.methodDecMap == null) {
                    curTypeInfo.methodDecMap = new LinkedHashMap<String, ArrayList<MethodInfo>>(1, 0.9f);
                }
                curTypeInfo.methodDecMap.put(methodName, methodList);
            }

            // Logger.log("curMethod: " + methodInfo);
            if (n.getParameters() != null) {
                for (Parameter p : n.getParameters()) {
                    getParameter(p);
                }
            }

            if (CONFIG_RAW) {
                // public int add ---> METHOD{[ID_SUFFIX],int,add}
                // System.out.println(n.getType().toString() + "--" + n.getType().getClass());
                OldNodeSequenceVisitingProcessing.addSTMethodNode(
                        MetricsVisitor.ID_SUFFIX + "," + n.getType().asString() + "," + n.getName(), nodeSequenceList)
                        .setPosition(n.getBegin(), null)
                        .oriNode = n;
            } else
                OldNodeSequenceVisitingProcessing.addSTMethodNode(n.getType().asString() + "," + n.getName(), nodeSequenceList)
                        .setPosition(n.getBegin(), null)
                        .oriNode = n;
            
            // --->
            //short nodeType = NodeSequenceConstant.METHOD;
            //NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addMethodDecNode(nodeType,
            //        nodeSequenceStack, curTypeInfo, methodInfo, nodeSequenceList);
            
            // (int a, int b) --->
            visitFormalParameters(n.getParameters(), arg);
            
            // {return a + b;} --->
            if (n.getBody().isPresent()) visit(n.getBody().get(), arg);

            // --->
            //OldNodeSequenceVisitingProcessing.addEndMethodDecNode(nodeType, nodeSequenceInfo.nodeSeqID,
            //        nodeSequenceStack, curTypeInfo, methodInfo, nodeSequenceList);
            // Logger.logDebug("Method Declaration:" + n);
            // Logger.logDebug("curTypeInfo.methodDecMap:" + curTypeInfo.methodDecMap);

            // ---> ENDMETHOD
            OldNodeSequenceVisitingProcessing.addENMethodNode(nodeSequenceList).setPosition(n.getEnd())
                    .oriNode = n;

            // Logger.log("method local variables: " + methodInfo.shortLocalVariableMap);

            methodInfo.synchronizeMethodInvocList();
            methodInfo.synchronizeNodeList();

            curMethodInfo = methodInfoStack.pop();

            // curParentNode = null;
            // parentNodeStack = new Stack<NodeInfo>();

            curNode = null;
            previousControlFlowNodeStack = new Stack<ArrayList<NodeInfo>>();

            DIC.put(MetricsVisitor.ID_SUFFIX + "", n.toString());
            METHOD_FILE.put(MetricsVisitor.ID_SUFFIX + "", fileInfo.filePath);
        }
    }
    
    // Eg: Point(int x, int y) {...}
    @Override
    public void visit(ConstructorDeclaration n, Object arg) {
        if (n.getBody() == null || n.getBody().toString().length() < 5) {
        } else {
            //fileInfo.numStatements++;
            MetricsVisitor.ID_SUFFIX++;
            curTypeInfo = typeStack.peek();
            fileInfo.numMethodDecs++;
            
            String methodName = n.getName().asString();
            ArrayList<String> paramsList = null;
            if (n.getParameters() != null) {
                if (n.getParameters().isNonEmpty()) {
                    paramsList = new ArrayList<String>();
                    for (Parameter p : n.getParameters()) {
                        paramsList.add(p.getType().asString() + (p.isVarArgs()? "[]": ""));
                    }
                }
            }
            
            // curTypeInfo = typeStack.peek();
            
            MethodInfo methodInfo = new MethodInfo(methodName, paramsList, curTypeInfo, fileInfo);
            curMethodInfo = methodInfo;
            
            if (curMethodInfo != null) {
                methodInfoStack.push(curMethodInfo);
            }
            
            if ((curTypeInfo.methodDecMap != null) && (curTypeInfo.methodDecMap.containsKey(methodName))) {
                curTypeInfo.methodDecMap.get(methodName).add(methodInfo);
            } else {
                ArrayList<MethodInfo> methodList = new ArrayList<MethodInfo>();
                methodList.add(methodInfo);
                if (curTypeInfo.methodDecMap == null) {
                    curTypeInfo.methodDecMap = new LinkedHashMap<String, ArrayList<MethodInfo>>(1, 0.9f);
                }
                curTypeInfo.methodDecMap.put(methodName, methodList);
            }
            
            if (n.getParameters() != null) {
                for (Parameter p : n.getParameters()) {
                    getParameter(p);
                }
            }
            
            // ---> CONSTRUCTOR
            short nodeType = NodeSequenceConstant.CONSTRUCTOR;
            NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addMethodDecNode(nodeType,
                    nodeSequenceStack, curTypeInfo, methodInfo, nodeSequenceList)
                    .setPosition(n.getBegin(), null);
            
            // (int x, int y) --->
            visitFormalParameters(n.getParameters(), arg);
            
            // {...} --->
            visit(n.getBody(), arg);
            
            // ---> CONSTRUCTOR
            //OldNodeSequenceVisitingProcessing.addEndMethodDecNode(nodeType, nodeSequenceInfo.nodeSeqID, 
            //        nodeSequenceStack, curTypeInfo, methodInfo, nodeSequenceList);
            
            methodInfo.synchronizeMethodInvocList();
            methodInfo.synchronizeNodeList();
            
            curMethodInfo = methodInfoStack.pop();
            
            curNode = null;
            previousControlFlowNodeStack = new Stack<ArrayList<NodeInfo>>();
    
            DIC.put(MetricsVisitor.ID_SUFFIX + "", n.toString());
            METHOD_FILE.put(MetricsVisitor.ID_SUFFIX + "", fileInfo.filePath);
        }
    }
    
    // Eg: (int a, int b)
    public void visitFormalParameters(NodeList<Parameter> n, Object arg) {
        Node parentNode = n.getParentNode().get();

	    // ( ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
                //.setPosition(getPositionOfStringFrom(parentNode.getBegin(), getStringWithoutComment(parentNode), "("));

        if (n.isNonEmpty()) {
            for (int i = 0; i < n.size() - 1; i++) {
                // a --->
                Parameter p = n.get(i);
                visit(p, arg);
                
                // , ---> SEPA(,)
                OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ',')
                        .setPosition(getPositionFrom(p.getEnd(), 1));
            }
            
            // b --->
            Parameter p = n.get(n.size() - 1);
            visit(p, arg);
        }
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
                //.setPosition(getPositionOfStringFrom(parentNode.getBegin(), getStringWithoutComment(parentNode), ")"));
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void visit(Parameter n, Object arg) {
        if (arg instanceof Set && n.isVarArgs()) {
            arg = new HashSet<String>((Set<String>)arg);
            ((Set<String>)arg).add("varargs");
        }
        doVisitType(n.getType(), arg);
        
        // x ---> VAR(int, x)
        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
                curID, n);
        String varName = n.getNameAsString();
        OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, varName, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getBegin(), n.getEnd());
        curID++;
    }
    
    // Eg: final int x = 3, y = 55;
    @SuppressWarnings("unchecked")
    @Override
    public void visit(VariableDeclarationExpr n, Object arg) {
        fileInfo.numStatements++;
        //Logger.log("VariableDeclarationExpr: " + n);
        // getVariable(n);

        // String assignType = n.getType().toString();
        NodeVisitProcessing.addTypeNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack, curID, n);
        // NodeSequenceVisitingProcessing.addAssignmentNode
        // (nodeInfo, assignType, nodeSequenceStack, curMethodInfo, curTypeInfo,
        // nodeSequenceList);

        curID++;
        if (arg instanceof Set && ((Set<String>)arg).contains("multiple_variables_declaration")) {
            Set<String> argClone = new HashSet<String>((Set<String>)arg);
            argClone.remove("multiple_variables_declaration");
            visit(n.getVariable(0), argClone);
            
            for (int i = 1; i < n.getVariables().size(); ++i) {
                // , ---> SEPA(,)
                OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ',')
                        .setPosition(getPositionFrom(n.getVariable(i - 1).getEnd(), 1));
                
                visit(n.getVariable(i), arg);
            }
        }
        else {
            n.getVariables().forEach(p -> visit(p, arg));
        }
    }
    
    // Eg: int x = y;
    @SuppressWarnings("unchecked")
    @Override
    public void visit(VariableDeclarator n, Object arg) {
        // Node parentNode = n.getParentNode();
        // Logger.logDebug("VariableDeclarator: " + n + "\tparent: " +
        // parentNode.getClass() + "\t" + curTypeInfo.typeName);
        
        if (!(arg instanceof Set && ((Set<String>)arg).contains("multiple_variables_declaration"))) {
            // int --->
            doVisitType(n.getType(), arg);
        }
        
        getVariable(n);

        // x ---> VAR(int, x)
        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
                curID, n);
        String varName = n.getName().asString();
        OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, varName, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList).setPosition(n.getName().getBegin(), n.getName().getEnd());
        curID++;
        
        Expression initializer = n.getInitializer().orElse(null);

        // = ---> ASSIGN(ASSIGN)
        if (initializer != null) {
            String assignType = Operator.ASSIGN.toString();
            NodeInfo assignNodeInfo = NodeVisitProcessing.addNewAssignNode(assignType, curMethodInfo, parentNodeStack,
                    previousControlFlowNodeStack, curID, n);

            OldNodeSequenceVisitingProcessing.addAssignmentNode(assignNodeInfo, assignType, nodeSequenceStack, curMethodInfo,
                    curTypeInfo, nodeSequenceList).setPosition(getPositionFrom(initializer.getBegin(), -1));
            curID++;
        }

        // y --->
        if (initializer != null) {
            doVisitExpression(initializer, arg);
        }
    }
    
    // Eg: {{1, 1}, {2, 2}} """in""" new int[][]{{1, 1}, {2, 2}};
    // Treats array initializer as null
    @Override
    public void visit(ArrayInitializerExpr n, Object arg) {
        //Logger.log("ArrayInitializerExpr :" + n.toString());
        //n.getValues().forEach(p -> doVisitExpression(p, arg));
        
        // {{1, 1}, {2, 2}} ---> LIT(null)
        visit(new NullLiteralExpr(), arg);
    }
    
    // Eg: static { a=3;} """in"""  class X { static { a=3; } }
    @Override
    public void visit(InitializerDeclaration n, Object arg) {
        // Logger.log("InitializerDeclaration: " + n);
        //fileInfo.numStatements++;
        fileInfo.numBlocks++;

        curTypeInfo = typeStack.peek();
        
        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.STATIC);
        curID++;
        curNode = nodeInfo;

        // static ---> STATIC
        if (n.isStatic()) {
            short nodeType = NodeSequenceConstant.STATIC;
            NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
                    curMethodInfo, curTypeInfo, nodeSequenceList)
                    .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 5));
        }

        // { a=3;} --->
        visit(n.getBody(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
    }
    
    // Eg: { ... }
    @Override
    public void visit(BlockStmt n, Object arg) {
	    Node parentNode = n.getParentNode().get();

        // { ---> OPBLK
        OldNodeSequenceVisitingProcessing.addOPBLKNode(nodeSequenceList);
                //.setPosition(getPositionOfStringFrom(parentNode.getBegin(), getStringWithoutComment(parentNode), "{"));
        fileInfo.numBlocks++;
        // Logger.log("BlockStmt: " + n.getClass() + "\t" +
        // n.getParentNode().getClass());

        // ... --->
        n.getStatements().forEach(p -> doVisitStatement(p, arg));
        
        // } ---> CLBLK
        OldNodeSequenceVisitingProcessing.addCLBLKNode(nodeSequenceList);
                //.setPosition(getLastPositionOfStringFrom(parentNode.getBegin(), getStringWithoutComment(parentNode), "}"));
    }
    
    // Eg: class Y { } """in""" class X { void m() { class Y { } } }
    @Override
    public void visit(LocalClassDeclarationStmt n, Object arg) {
        visit(n.getClassDeclaration(), arg);
    }
    
    // Eg: ;
    @Override
    public void visit(EmptyStmt n, Object arg) {
        fileInfo.numStatements++;
    }
    
    // Eg: label123: println("continuing");
    // Ignores label
    @Override
    public void visit(LabeledStmt n, Object arg) {
        // Logger.log("LabeledStmt: " + n);
        doVisitStatement(n.getStatement(), arg);
    }
    
    // SONNV mark it as a normal statement
    @Override
    public void visit(ExpressionStmt n, Object arg) {
        fileInfo.numStatements++;
        
        // ---> STSTM{EXPR}
        // Logger.log("ExpressionStmt: " + n);
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.EXPR, nodeSequenceList)
                .setPosition(n.getBegin());
        
        doVisitExpression(n.getExpression(), arg);
        
        // ---> ENSTM{EXPR}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.EXPR, nodeSequenceList)
                .setPosition(n.getEnd());
    }
    
    // Eg: if(a==5) hurray() else boo();
    @Override
    public void visit(IfStmt n, Object arg) {
        curTypeInfo = typeStack.peek();
        // TODO: should make better branching
        fileInfo.numBranches++;
        fileInfo.numIfControls++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.IF);
        curID++;
        curNode = nodeInfo;

        // if ---> STSTM{IF}
        // For If branch
        short nodeType = NodeSequenceConstant.IF;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 1));
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        
        // (a==5) --->
        // For Condition
        // System.out.println(n.getCondition() + "-" + n.getCondition().getClass());
        visit(new EnclosedExpr(n.getCondition()), arg);

        // hurray() --->
        doVisitStatement(n.getThenStmt(), arg);
        
        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // ---> ENSTM{IF}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getThenStmt().getEnd());
        
        if (n.getElseStmt().isPresent()) {
            // else ---> STSTM{ELSE}
            // For Else branch
            short nodeTypeElse = NodeSequenceConstant.ELSE;
            OldNodeSequenceVisitingProcessing.addSTStmNode(nodeTypeElse, nodeSequenceList)
                    .setPosition(getPositionFrom(n.getElseStmt().get().getBegin(), -1));
            
            // --->
            //NodeSequenceInfo nodeSeqInfoElse = OldNodeSequenceVisitingProcessing.addControlNode(nodeTypeElse,
            //        nodeSequenceStack, curMethodInfo, curTypeInfo, nodeSequenceList);

            // boo() --->
            doVisitStatement(n.getElseStmt().get(), arg);
            
            // --->
            //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeTypeElse, nodeSeqInfoElse.nodeSeqID,
            //        nodeSequenceStack, curMethodInfo, curTypeInfo, nodeSequenceList);
            
            // ---> ENSTM{ELSE}
            OldNodeSequenceVisitingProcessing.addENStmNode(nodeTypeElse, nodeSequenceList)
                    .setPosition(n.getElseStmt().get().getEnd());
        }

        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
    }
    
    // TODO: may handle this in the future
    // Eg: assert dead : "Wasn't expecting to be dead here";
    @Override
    public void visit(AssertStmt n, Object arg) {
        fileInfo.numStatements++;
        //super.visit(n, arg);
    }
    
    // switch(x) { case BANANA,PEAR -> println("uhuh"); default -> println("nope"); };
    @Override
    public void visit(SwitchStmt n, Object arg) {
        curTypeInfo = typeStack.peek();
        if (n.getEntries() != null) {
            fileInfo.numBranches += n.getEntries().size();
        }
        fileInfo.numSwitchControls++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.SWITCH);
        curID++;
        curNode = nodeInfo;

        // switch ---> STSTM{SWITCH}
        short nodeType = NodeSequenceConstant.SWITCH;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 5));
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        
        // (x) --->
        visit(new EnclosedExpr(n.getSelector()), arg);

        // { ---> OPBLK
        OldNodeSequenceVisitingProcessing.addOPBLKNode(nodeSequenceList);
                //.setPosition(getPositionOfStringFrom(n.getBegin(), getStringWithoutComment(n), "{"));
        
        // case BANANA,PEAR -> println("uhuh"); default -> println("nope"); --->
        visitSwitchBlockStatementGroups(n.getEntries(), arg);
        
        // } ---> CLBLK
        OldNodeSequenceVisitingProcessing.addCLBLKNode(nodeSequenceList);
                //.setPosition(getLastPositionOfStringFrom(n.getBegin(), getStringWithoutComment(n), "}"));

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{SWITCH}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList).setPosition(n.getEnd());
    }
    
    // Eg: While (i < 7) {++i;}
    @Override
    public void visit(WhileStmt n, Object arg) {
        curTypeInfo = typeStack.peek();
        fileInfo.numWhileControls++;
        fileInfo.numStatements++;
        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.WHILE);
        curID++;
        curNode = nodeInfo;

        short nodeType = NodeSequenceConstant.WHILE;
        
        // while ---> STSTM{WHILE}
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 4));
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //      curMethodInfo, curTypeInfo, nodeSequenceList);

        // (i < 7) --->
        visit(new EnclosedExpr(n.getCondition()), arg);

        // {++i;} --->
        doVisitStatement(n.getBody(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //      curMethodInfo, curTypeInfo, nodeSequenceList);

        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{WHILE}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList).setPosition(n.getEnd());
    }
    
    // Eg: do { ... } while ( a==0 );
    @Override
    public void visit(DoStmt n, Object arg) {
        curTypeInfo = typeStack.peek();
        fileInfo.numDoControls++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.DO);
        curID++;
        curNode = nodeInfo;

        short nodeType = NodeSequenceConstant.DO;

        // do ---> STSTM{DO}
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 1));

        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // { ... } --->
        doVisitStatement(n.getBody(), arg);
        
        // while ( a==0 ) --->
        visit(new EnclosedExpr(n.getCondition()), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // OldNodeSequenceVisitingProcessing.addOPBLKNode(nodeSequenceList);

        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);

        // OldNodeSequenceVisitingProcessing.addCLBLKNode(nodeSequenceList);

        // ---> ENSTM{DO}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList).setPosition(n.getEnd());
    }
    
    // Eg: for(int a=3, b=5; a<99; a++, b++) {hello();}
    @Override
    public void visit(ForStmt n, Object arg) {
        curTypeInfo = typeStack.peek();
        fileInfo.numForControls++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.FOR);
        curID++;
        curNode = nodeInfo;

        String content = getStringWithoutComment(n);

        // for ---> STSTM{FOR}
        short nodeType = NodeSequenceConstant.FOR;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 2));
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // super.visit(n, arg);
        
        // ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
                //.setPosition(getPositionOfStringFrom(n.getBegin(), content, "("));
        
        // int a=3, b=5 --->
        // Init
        if (n.getInitialization() != null) {
            @SuppressWarnings("unchecked")
            Set<String> argClone = (arg instanceof Set)? new HashSet<String>((Set<String>)arg): new HashSet<String>();
            argClone.add("multiple_variables_declaration");
            visitExpressionList(n.getInitialization(), argClone);
        }
        
        // ; ---> SEPA(;)
        OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ';');
                //.setPosition(getPositionOfStringFrom(n.getBegin(), content, ";"));
        
        // a<99 --->
        // Condition
        if (n.getCompare().isPresent()) {
            doVisitExpression(n.getCompare().get(), arg);
        }
        
        // ; ---> SEPA(;)
        OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ';');
                //.setPosition(getLastPositionOfStringFrom(n.getBegin(), content.substring(0, content.indexOf('{')), ";"));

        // a++, b++ --->
        // Update
        if (n.getUpdate() != null) {
            visitExpressionList(n.getUpdate(), arg);
        }
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
                //.setPosition(getLastPositionOfStringFrom(n.getBegin(), content.substring(0, content.indexOf('{')), ")"));

        // {hello();} --->
        doVisitStatement(n.getBody(), arg);
        
        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{FOR}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList).setPosition(n.getEnd());
    }
    
    // Eg: for(Object o: objects) { ... }
    @Override
    public void visit(ForEachStmt n, Object arg) {
        fileInfo.numForEachControls++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.FOREACH);
        curID++;
        curNode = nodeInfo;
        curTypeInfo = typeStack.peek();
        short nodeType = NodeSequenceConstant.FOREACH;

        String content = getStringWithoutComment(n);
        
        // for ---> STSTM{FOREACH}
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList)
                .setPosition(n.getBegin(), getPositionFrom(n.getBegin(), 2));
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // super.visit(n, arg);
        
        // ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
                //.setPosition(getPositionOfStringFrom(n.getBegin(), content, "("));
        
        // Object o --->
        visit(n.getVariable(), arg);
        
        // : ---> SEPA(:)
        OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ':');
        
        // objects --->
        doVisitExpression(n.getIterable(), arg);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
                //.setPosition(getLastPositionOfStringFrom(n.getBegin(), content.substring(0, content.indexOf('{')), ")"));
        
        // { ... } --->
        doVisitStatement(n.getBody(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{FOREACH}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList).setPosition(n.getEnd());
    }
    
    // Eg: break 123+456;
    // Ignores label
    @Override
    public void visit(BreakStmt n, Object arg) {
        // break ---> STSTM{BREAK}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.BREAK, nodeSequenceList);
        
        if (n.getLabel().isPresent()) {
            // 123+456 ---> BREAK(123+456)
            //String label = n.getLabel().get().asString();
            //OldNodeSequenceVisitingProcessing.addOtherNode(NodeSequenceConstant.BREAK, label, nodeSequenceStack, curMethodInfo,
            //        curTypeInfo, nodeSequenceList);
        }
        
        // ---> ENSTM{BREAK}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.BREAK, nodeSequenceList);
    }
    
    // Eg: yield 123+456;
    @Override
    public void visit(YieldStmt n, Object arg) {
        // yield ---> STSTM{YIELD}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.YIELD, nodeSequenceList);
        
        // 123+456 --->
        doVisitExpression(n.getExpression(), arg);
        
        // ---> ENSTM{YIELD}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.YIELD, nodeSequenceList);
    }
    
    // Eg: continue brains;
    // Ignores label
    @Override
    public void visit(ContinueStmt n, Object arg) {
        fileInfo.numStatements++;
        
        // continue ---> STSTM{CONTINUE}
        short nodeType = NodeSequenceConstant.CONTINUE;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList);
        
        if (n.getLabel().isPresent()) {
            // brains ---> CONTINUE(brains)
            //String label = n.getLabel().get().asString();
            //OldNodeSequenceVisitingProcessing.addOtherNode(nodeType, label, nodeSequenceStack, curMethodInfo, curTypeInfo,
            //        nodeSequenceList);
        }
        
        // ---> ENSTM{CONTINUE}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList);
    }
    
    // Eg: return 1 + 1;
    @Override
    public void visit(ReturnStmt n, Object arg) {
        // Logger.log("ReturnStmt: " + n);
        
        // return ---> STSTM{RETURN}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.RETURN, nodeSequenceList);
        fileInfo.numStatements++;

        // 1 + 1 --->
        if (n.getExpression().isPresent()) doVisitExpression(n.getExpression().get(), arg);
        
        // ---> ENSTM{RETURN}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.RETURN, nodeSequenceList);
    }
    
    // Eg: throw new Exception();
    @Override
    public void visit(ThrowStmt n, Object arg) {
        // Logger.log("ThrowStmt: " + n);
        fileInfo.numThrows++;
        fileInfo.numStatements++;
        
        // throw ---> STSTM{THROW}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.THROW, nodeSequenceList);
        
        doVisitExpression(n.getExpression(), arg);
        
        // ---> ENSTM{THROW}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.THROW, nodeSequenceList);
    }
    
    // Eg: synchronized (instance) {instance = new A();}
    @Override
    public void visit(SynchronizedStmt n, Object arg) {
        // Logger.log("SynchronizedStmt: " + n);

        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.SYNC);
        curID++;
        curNode = nodeInfo;

        // synchronized ---> STSTM{SYNC}
        short nodeType = NodeSequenceConstant.SYNC;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList);
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // (instance) --->
        //System.out.println(n.getExpr() + "-" + n.getExpr().getClass());
        visit(new EnclosedExpr(n.getExpression()), arg);

        // {instance = new A();} --->
        visit(n.getBody(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{SYNC}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList);
    }
    
    // Eg: try (InputStream i = new FileInputStream("file")) {...
    //     } catch (IOException|NullPointerException e) {...
    //     } finally {...}
    @Override
    public void visit(TryStmt n, Object arg) {

        fileInfo.numTrys++;
        fileInfo.numStatements++;

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.TRY);
        curID++;
        curNode = nodeInfo;

        // try ---> STSTM{TRY}
        short nodeType = NodeSequenceConstant.TRY;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList);
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        
        // (InputStream i = new FileInputStream("file")) --->
        if (n.getResources().isNonEmpty()) {
            visitResourceSpecification(n.getResources(), arg);
        }

        // {...} --->
        visit(n.getTryBlock(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        
        // ---> ENSTM{TRY}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList);

        // catch (IOException|NullPointerException e) {...} --->
        visitCatches(n.getCatchClauses(), arg);

        // finally {...} --->
        if (n.getFinallyBlock().isPresent()) {
            visitFinallyBlock(n.getFinallyBlock().get(), arg);
        }
    }
    
    // Eg: catch (IOException|NullPointerException e) {...}
    public void visitCatches(NodeList<CatchClause> n, Object arg) {
        for (CatchClause c : n) visit(c, arg);
    }
    
    // Eg: catch (Exception e) { ... } """in""" try { ... } catch (Exception e) { ... }
    @Override
    public void visit(CatchClause n, Object arg) {
        fileInfo.numCatches++;
        fileInfo.numStatements++;

        getParameter(n.getParameter());

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.CATCH);
        curID++;
        curNode = nodeInfo;

        // catch ---> STSTM{CATCH}
        short nodeType = NodeSequenceConstant.CATCH;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList);
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // super.visit(n, arg);
        
        // ( ---> OPEN_PART
        // System.out.println(n.getExcept() +"---" + n.getExcept().);
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // Exception e --->
        doVisit(n.getParameter(), arg);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);

        // { ... } --->
        doVisitStatement(n.getBody(), arg);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        
        // ---> ENSTM{CATCH}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList);
    }
    
    // Eg: IOException | NullPointerException """in""" 
    // try {...} catch(IOException | NullPointerException ex) {...}
    @Override
    public void visit(UnionType n, Object arg) {
        for (int i = 0; i < n.getElements().size() - 1; i++) {
            // IOException --->
            doVisitType(n.getElements().get(i), arg);
            
            // | ---> SEPA(|)
            OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, '|');
        }
        
        // NullPointerException --->
        doVisitType(n.getElements().get(n.getElements().size() - 1), arg);
    }
    
    // Eg: finally {...}
    public void visitFinallyBlock(BlockStmt n, Object arg) {
        // finally { ---> STSTM{FINALLY}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.FINALLY, nodeSequenceList);
        
        // ... --->
        visit(n, arg);
        
        // } ---> ENSTM{FINALLY}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.FINALLY, nodeSequenceList);
    }
    
    // Eg: (InputStream i = new FileInputStream("file"))
    public void visitResourceSpecification(NodeList<Expression> n, Object arg) {
        // ( ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        for (int i = 0; i < n.size() - 1; ++i) {
            // InputStream i = new FileInputStream("file") --->
            doVisitExpression(n.get(i), arg);
            
            // ; ---> SEPA(;)
            OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ';');
        }
        doVisitExpression(n.get(n.size() - 1), arg);
        
        // ) ----> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
    }
    
    // Eg: case BANANA,PEAR -> println("uhuh"); default -> println("nope");
    public void visitSwitchBlockStatementGroups(NodeList<SwitchEntry> n, Object arg) {
        for (SwitchEntry entry : n) {
            doVisit(entry, arg);
        }
    }
    
    // TODO: handle arrow case with yield statement
    // Eg: case 3, 10+10 -> throw new Exception();
    // Converts arrow case to normal case with colon
    @Override
    public void visit(SwitchEntry n, Object arg) {
        fileInfo.numStatements++;
        // Logger.log("SwitchEntryStmt: " + n);

        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.CASE);
        curID++;
        curNode = nodeInfo;

        // case ---> STSTM{CASE}
        short nodeType = n.getLabels().isNonEmpty()? NodeSequenceConstant.CASE : NodeSequenceConstant.CASE_DEFAULT;
        OldNodeSequenceVisitingProcessing.addSTStmNode(nodeType, nodeSequenceList);
        
        // --->
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // super.visit(n, arg);
        if (n.getLabels() != null) {
            // 3, 10+10 --->
            for (Expression label : n.getLabels()) {
                doVisitExpression(label, arg);
            }
            
            // -> ---> CASE_PART
            OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.CASE_PART, nodeSequenceList, true);
            
            if (n.getStatements() != null && n.getStatements().isNonEmpty()) {
                // throw new Exception() --->
                for (Statement stm : n.getStatements()) {
                    doVisitStatement(stm, arg);
                }
                
                // Adds a break statement at the end due to converting arrow to colon
                if (n.getType() == SwitchEntry.Type.BLOCK || n.getType() == SwitchEntry.Type.THROWS_STATEMENT) {
                    doVisitStatement(new BreakStmt(), arg);
                }
            }
        }

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);
        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ---> ENSTM{CASE}
        OldNodeSequenceVisitingProcessing.addENStmNode(nodeType, nodeSequenceList);
    }
    
    // Not supported by JavaParser
    // TODO: may handle this in the future
    // int a = switch(x) { case 5,6: yield 20; default: yield 5+5; };
    @Override
    public void visit(SwitchExpr n, Object arg) {
        visit(new NullLiteralExpr(), arg);
    }
    
    // Eg: watch.time+=500
    @Override
    public void visit(AssignExpr n, Object arg) {
        // Logger.logDebug("AssignExpr: " + n +"\tOperator: " + n.getOperator() + "\t" +
        // n.getTarget().getClass());
        curTypeInfo = typeStack.peek();

        fileInfo.numStatements++;

        // watch.time --->
        doVisitExpression(n.getTarget(), arg);

        // += ---> ASSIGN(PLUS)
        String assignType = n.getOperator().toString();
        NodeInfo nodeInfo = NodeVisitProcessing.addNewAssignNode(assignType, curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addAssignmentNode(nodeInfo, assignType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList);

        curID++;

        // 500 --->
        doVisitExpression(n.getValue(), arg);
    }
    
    // Eg: b==0?x:y
    @Override
    public void visit(ConditionalExpr n, Object arg) {
        // System.out.println(n);
        fileInfo.numBranches++;
        fileInfo.numStatements++;
        curTypeInfo = typeStack.peek();
        NodeInfo nodeInfo = NodeVisitProcessing.addNewControlNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, ControlInfo.CONDITIONAL);
        curID++;
        curNode = nodeInfo;

        // --->
        //short nodeType = NodeSequenceConstant.COND;
        //NodeSequenceInfo nodeSeqInfo = OldNodeSequenceVisitingProcessing.addControlNode(nodeType, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        // ---> CEXP
        OldNodeSequenceVisitingProcessing.addConditionalExprNode(NodeSequenceConstant.CONDITIONAL_EXPR,
                nodeSequenceList);
        
        // ---> OPEN_PART
        //OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // b==0 --->
        doVisitExpression(n.getCondition(), arg);
        
        // ? ---> SEPA(?)
        OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, '?');
        
        // x --->
        doVisitExpression(n.getThenExpr(), arg);
        
        // : ---> SEPA(:)
        OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ':');
        
        // y --->
        doVisitExpression(n.getElseExpr(), arg);
        
        // ---> CLOSE_PART
        //OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);

        // --->
        //OldNodeSequenceVisitingProcessing.addEndControlNode(nodeType, nodeSeqInfo.nodeSeqID, nodeSequenceStack,
        //        curMethodInfo, curTypeInfo, nodeSequenceList);

        NodeVisitProcessing.removeControlNodeInfo(nodeInfo, parentNodeStack, previousControlFlowNodeStack);
    }
    
    // Eg: tool instanceof Drill
    @Override
    public void visit(InstanceOfExpr n, Object arg) {
        // tool --->
        doVisitExpression(n.getExpression(), arg);
        
        // instanceof ---> OP(INSTANCEOF)
        String operatorType = "INSTANCEOF";
        OldNodeSequenceVisitingProcessing.addOperatorNode(operatorType, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList);
        
        // Drill --->
        doVisitType(n.getType(), arg);
    }
    
    // Eg: a && b
    @Override
    public void visit(BinaryExpr n, Object arg) {

        // Logger.log("BinaryExpr: " + n.getOperator());

        // a --->
        doVisitExpression(n.getLeft(), arg);
        
        // && ---> OP(AND)
        String operatorType = n.getOperator().toString();
        OldNodeSequenceVisitingProcessing.addOperatorNode(operatorType, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList);
        
        // b --->
        doVisitExpression(n.getRight(), arg);
    }
    
    // Eg: i++
    @Override
    public void visit(UnaryExpr n, Object arg) {
        //Logger.log("UnaryExpr:" + n );

        String operatorType = n.getOperator().toString();

        // Eg: x++
        if (n.isPostfix()) {
            // x -->
            doVisitExpression(n.getExpression(), arg);
            
            // ++ ---> UOP(posIncrement)
            OldNodeSequenceVisitingProcessing.addUOperatorNode(operatorType, nodeSequenceStack, curMethodInfo,
                    curTypeInfo, nodeSequenceList);
        } 
        // Eg: -x
        else if (n.isPrefix()) {
            // - ---> UOP(not)
            OldNodeSequenceVisitingProcessing.addUOperatorNode(operatorType, nodeSequenceStack, curMethodInfo,
                    curTypeInfo, nodeSequenceList);
            
            // x --->
            doVisitExpression(n.getExpression(), arg);
        }
    }
    
    // Eg: (long) """in""" (long)15
    @Override
    public void visit(CastExpr n, Object arg) {
        String typeCast = n.getType().asString();
        OldNodeSequenceVisitingProcessing.addOtherNode(NodeSequenceConstant.CAST, typeCast, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList);
        
        doVisitExpression(n.getExpression(), arg);
    }
    
    // Eg: 0x01
    @Override
    public void visit(IntegerLiteralExpr n, Object arg) {
        // 0x01 ---> LIT(num)
        String literalType = "num";
        
        // 0 ---> LIT(zero)
        //if (n.getValue().equals("0")) literalType = "zero";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: 0B10101010L
    @Override
    public void visit(LongLiteralExpr n, Object arg) {
        // 0B10101010L ---> LIT(num)
        String literalType = "num";
        
        // 0L ---> LIT(zero)
        //if (n.getValue().equals("0L")) literalType = "zero";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: 0x4.5p1f
    @Override
    public void visit(DoubleLiteralExpr n, Object arg) {
        // 0x4.5p1f ---> LIT(double)
        String literalType = "num";
        
        // 0f ---> LIT(zero)
        //if (n.getValue().equals("0f")) literalType = "zero";
        
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: '\177'
    @Override
    public void visit(CharLiteralExpr n, Object arg) {
        // '\177' ---> LIT(num)
        String literalType = "num";
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }

    // Eg: "Hello World!"
    @Override
    public void visit(StringLiteralExpr n, Object arg) {
        // "Hello World!" ---> LIT(String)
        // Logger.log("StringLiteralExpr: " + n);
        String literalType = "String";
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: true ---> LIT(boolean)
    @Override
    public void visit(BooleanLiteralExpr n, Object arg) {
        String literalType = "boolean";
        // String literalType = "true";
        //
        // if (n.getValue()==false){
        // literalType = "false";
        //
        // }
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);

        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: null
    @Override
    public void visit(NullLiteralExpr n, Object arg) {
        // null ---> LIT(null)
        // Logger.log("NullLiteralExpr: " + n);
        String literalType = "null";
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: (1+1)
    @Override
    public void visit(EnclosedExpr n, Object arg) {
        //Logger.log("EnclosedExpr: " + n);
        
        // ( ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // 1+1 --->
        doVisitExpression(n.getInner(), arg);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
    }
    
    // Eg: x, y """in""" go(x, y);
    public void visitExpressionList(NodeList<Expression> n, Object arg) {
        isParam = true;
        if (n.isNonEmpty()) {
            //System.out.println(n.get(0));
            for (int i = 0; i < n.size() - 1; i++) {
                // x --->
                Expression methodArg = n.get(i);
                doVisitExpression(methodArg, arg);
                
                // , ---> SEPA(,)
                OldNodeSequenceVisitingProcessing.addSEPANode(NodeSequenceConstant.SEPA, nodeSequenceList, ',')
                        .oriNode = methodArg;
            }
            
            // y --->
            Expression methodArg = n.get(n.size() - 1);
            doVisitExpression(methodArg, arg);
        }
        isParam = false;
    }
    
    /**
     * @see     #visit(ClassOrInterfaceDeclaration, Object)
     */
    // Eg: World.this """in""" World.this.greet()
    @Override
    public void visit(ThisExpr n, Object arg) {
        Node parentNode = n.getParentNode().orElse(null);
        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
                curID, parentNode);
        
        if (n.getTypeName().isPresent()) {
            // World.this ---> VAR(World,World.this)
            OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, n.getTypeName().get().asString() + ".this", nodeSequenceStack, curMethodInfo, curTypeInfo,
                    nodeSequenceList);
        }
        else {
            // this ---> VAR(World,this)
            OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, "this", nodeSequenceStack, curMethodInfo, curTypeInfo,
                    nodeSequenceList);
        }
    }
    
    /**
     * @see     #visit(ClassOrInterfaceDeclaration, Object)
     */
    // Eg: World.super """in""" World.super.greet()
    @Override
    public void visit(SuperExpr n, Object arg) {
        //Logger.log("SuperExpr: " + n);
        //checkMethodCallExpr(n);
        Node parentNode = n.getParentNode().orElse(null);
        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
                curID, parentNode);
        
        if (n.getTypeName().isPresent()) {
            // World.super ---> VAR(SolarSystem,World.super)
            OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, n.getTypeName().get().asString() + ".super", nodeSequenceStack, curMethodInfo, curTypeInfo,
                    nodeSequenceList);
        }
        else {
            // super ---> VAR(SolarSystem,super)
            OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, "super", nodeSequenceStack, curMethodInfo, curTypeInfo,
                    nodeSequenceList);
        }
    }
    
    // TODO: handle type arguments more specifically
    // Eg: new HashMap.Entry<String, Long>(x, y) {public String getKey() {return null;}};
    @Override
    public void visit(ObjectCreationExpr n, Object arg) {
        fileInfo.numStatements++;

        //Node parentNode = n.getParentNode();

        //TODO: Consider this
        //String methodName = n.getTypeAsString();
        String methodName = n.getType().getName().asString();
        // HashMap.Entry
        if (methodName.lastIndexOf('>') == methodName.length() - 1)
            for (int i = methodName.length() - 1, balance = 0; i >= 0; --i) {
                if (methodName.charAt(i) == '>') ++balance;
                if (methodName.charAt(i) == '<') --balance;
                if (balance == 0) {
                    methodName = methodName.substring(0, i);
                    break;
                }
            }

        String varName = "";
        // if (parentNode instanceof VariableDeclarator){
        // varName = ((VariableDeclarator) parentNode).getId().toString();
        // }
        // else if (parentNode instanceof AssignExpr){
        // varName = ((AssignExpr) parentNode).getTarget().toString();
        // }

        //TODO: Consider this
        if (varName == "") {
            //varName = n.getTypeAsString();
            varName = n.getType().getName().asString();
        }
        // Logger.log("ObjectCreationExpr: " + n +" \t " + n.getType() + "\t " +
        // n.getType().getName()
        // + "\n\t parent: " + parentNode + "\n\t" + parentNode.getClass() + "\t" +
        // varName) ;

        // TODO: should check inner method here
        boolean isInner = false;

        ArrayList<String> parameterList = null;
        if (n.getArguments() != null) {
            parameterList = new ArrayList<String>();
            for (Expression exp : n.getArguments()) {
                parameterList.add(exp.toString());
            }
        }

        MethodInvocInfo methodInvocInfo = new MethodInvocInfo(isInner, methodName, varName, parameterList);

        if (curMethodInfo != null) {
            if (curMethodInfo.methodInvocList == null) {
                MethodInfo.methodInvocTmpList = new ArrayList<MethodInvocInfo>(1);
            }
            MethodInfo.methodInvocTmpList.add(methodInvocInfo);
        } else {
            curTypeInfo = typeStack.peek();
            if (curTypeInfo.methodInvocList == null) {
                curTypeInfo.methodInvocList = new ArrayList<MethodInvocInfo>(1);
            }
            curTypeInfo.methodInvocList.add(methodInvocInfo);
        }

        NodeInfo nodeInfo = NodeVisitProcessing.addNewInvocNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, methodInvocInfo, n);

        curID++;
        curNode = nodeInfo;
        // new HashMap.Entry<String, Long> ---> C_CALL(Entry<String, Long>,Entry)
        OldNodeSequenceVisitingProcessing.addConstructorNode(nodeInfo, varName, methodName, nodeSequenceStack,
                curMethodInfo, curTypeInfo, nodeSequenceList).oriNode = n;

        // super.visit(n, arg);

        TypeInfo typeInfo = new TypeInfo();
        typeInfo.typeName = n.getType().getName().asString();
        typeInfo.packageDec = fileInfo.packageDec;

        // Logger.log("ClassOrInterfaceDeclaration type: " + typeInfo.accessModType );

        typeInfo.fileInfo = fileInfo;

        Node ascendant = getAscendantTypeMethod(n);
        if ((ascendant instanceof ClassOrInterfaceDeclaration) || (ascendant instanceof EnumDeclaration)
                || (ascendant instanceof AnnotationDeclaration)) {
            typeInfo.parentInfo = curTypeInfo;
        } else if ((ascendant instanceof MethodDeclaration) || (ascendant instanceof ConstructorDeclaration)) {
            typeInfo.parentInfo = curMethodInfo;

        }

        typeStack.push(typeInfo);
        
        // Add this variable for current class
        addVariableToScope(n, typeInfo.typeName.intern(), "this");
        addVariableToScope(n, typeInfo.typeName.intern(), typeInfo.typeName.intern() + ".this");
        
        // ( ---> OPEN_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // x, y --->
        visitExpressionList(n.getArguments(), arg);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);

        // {public String getKey() {return null;}} --->
        if (n.getAnonymousClassBody().isPresent()) {
            // ---> CLASS{START,Entry}
            NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addClassNode(methodName,
                    nodeSequenceStack, typeInfo, nodeSequenceList);
            nodeSequenceInfo.oriNode = n;
            
            // {...} --->
            visitClassBody(n.getAnonymousClassBody().get(), arg);
            
            // ---> CLASS{END,Entry}
            OldNodeSequenceVisitingProcessing.addEndClassNode(methodName, nodeSequenceInfo.nodeSeqID,
                    nodeSequenceStack, typeInfo, nodeSequenceList);
        } else {
            
        }
        
        typeStack.pop();

        // Logger.logDebug("ObjectCreationExpr: " + n +
        // "\t" + n.getAnonymousClassBody() + "\t" + fileInfo.filePath);

        NodeVisitProcessing.removeInvocNodeInfo(n, nodeInfo, parentNodeStack, previousControlFlowNodeStack);
    }
    
    // Eg: new int[][]{{1},{2,3}}
    @Override
    public void visit(ArrayCreationExpr n, Object arg) {
        //Logger.log("ArrayCreationExpr :" + n.toString());
        fileInfo.numStatements++;

        String methodName = n.getElementType().asString();

        String varName = "";

        if (varName == "") {
            varName = methodName;
        }

        varName = "Array_" + varName;
        // Logger.log("ObjectCreationExpr: " + n +" \t " + n.getType() + "\t " +
        // n.getType().getName()
        // + "\n\t parent: " + parentNode + "\n\t" + parentNode.getClass() + "\t" +
        // varName) ;
        // TODO: should check inner method here
        boolean isInner = false;

        ArrayList<String> parameterList = null;

        MethodInvocInfo methodInvocInfo = new MethodInvocInfo(isInner, methodName, varName, parameterList);

        if (curMethodInfo != null) {
            if (curMethodInfo.methodInvocList == null) {
                MethodInfo.methodInvocTmpList = new ArrayList<MethodInvocInfo>(1);
            }
            MethodInfo.methodInvocTmpList.add(methodInvocInfo);
        } else {
            curTypeInfo = typeStack.peek();
            if (curTypeInfo.methodInvocList == null) {
                curTypeInfo.methodInvocList = new ArrayList<MethodInvocInfo>(1);
            }
            curTypeInfo.methodInvocList.add(methodInvocInfo);
        }

        NodeInfo nodeInfo = NodeVisitProcessing.addNewInvocNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, methodInvocInfo, n);

        curID++;
        curNode = nodeInfo;

        // new int ---> C_CALL(Array_int,int)
        OldNodeSequenceVisitingProcessing.addConstructorNode(nodeInfo, varName, methodName, nodeSequenceStack,
                curMethodInfo, curTypeInfo, nodeSequenceList).oriNode = n;

        for (ArrayCreationLevel level : n.getLevels()) {
            // [ ---> OPEN_PART
            OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
            visit(level, arg);
            // ] ---> CLOSE_PART
            OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
        }
        
        // Initializer is ignored for now, may be added later.
        // {{1},{2,3}} --->
        //if (n.getInitializer().isPresent()) doVisitExpression(n.getInitializer().get(), arg);

        NodeVisitProcessing.removeInvocNodeInfo(n, nodeInfo, parentNodeStack, previousControlFlowNodeStack);
    }
    
    /**
     * @see     #visit(ArrayInitializerExpr, Object)
     */
    // Eg: 1 """in""" new int[1][2];
    @Override
    public void visit(ArrayCreationLevel n, Object arg) {
        if (n.getDimension().isPresent()) {
            doVisitExpression(n.getDimension().get(), arg);
        }
        // Adds an integer literal as array initializer is ignored.
        else {
            visit(new IntegerLiteralExpr("1"), arg);
        }
    }
    
    // Eg: this(1, 2); """in""" class X { X() { this(1, 2); } }
    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
        //Logger.log("ExplicitConstructorInvocationStmt: " + n + "\t" + n.getExpr() + "\t" + n.getArgs());

        curTypeInfo = typeStack.peek();
        fileInfo.numStatements++;

        String nodeStr = n.toString();
        
        String methodStr = "";
        if (n.getExpression().isPresent()) methodStr += n.getExpression().get() + ".";
        methodStr += n.isThis() ? "this" : "super";
        methodStr += nodeStr.substring(nodeStr.lastIndexOf("("));

        String varName = n.isThis() ? "this" : "super";

        String attachedType = curTypeInfo.typeName;

        boolean isInner = n.isThis();

        ArrayList<String> parameterList = null;
        if (n.getArguments() != null) {
            parameterList = new ArrayList<String>();
            for (Expression exp : n.getArguments()) {
                parameterList.add(exp.toString());
            }
        }

        String methodName = methodStr.substring(0, methodStr.indexOf("("));

        // TODO: should check if it is correct? If for this, should call constructor for this class
        // what about "super"?
        //if (methodStr.startsWith("this")) {
        //    varName = curTypeInfo.typeName;
        //    attachedType = curTypeInfo.typeName;
        //    methodName = varName;
        //}

        MethodInvocInfo methodInvocInfo = new MethodInvocInfo(isInner, methodName, varName, parameterList);

        if (curMethodInfo != null) {
            if (curMethodInfo.methodInvocList == null) {
                MethodInfo.methodInvocTmpList = new ArrayList<MethodInvocInfo>(1);
            }
            MethodInfo.methodInvocTmpList.add(methodInvocInfo);
        } else {
            curTypeInfo = typeStack.peek();
            if (curTypeInfo.methodInvocList == null) {
                curTypeInfo.methodInvocList = new ArrayList<MethodInvocInfo>(1);
            }
            curTypeInfo.methodInvocList.add(methodInvocInfo);
        }

        NodeInfo nodeInfo = NodeVisitProcessing.addNewInvocNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, methodInvocInfo, n);

        curID++;
        curNode = nodeInfo;

        int numOfArgs = 0;
        if (n.getArguments() != null) {
            numOfArgs = n.getArguments().size();
        }

        // this ---> STSTM{EXPL_CONSTR}
        OldNodeSequenceVisitingProcessing.addSTStmNode(NodeSequenceConstant.EXPL_CONSTR, nodeSequenceList);
        
        // ---> M_ACCESS(X, this, 2)
        OldNodeSequenceVisitingProcessing.addMethodAccessNode(nodeInfo, varName, attachedType, methodName,
                nodeSequenceStack, curMethodInfo, curTypeInfo, nodeSequenceList, numOfArgs).oriNode = n;
        
        // ( ---> OPEN_PART
        // Start arguments
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // 1, 2 --->
        visitExpressionList(n.getArguments(), arg);
        NodeVisitProcessing.removeInvocNodeInfo(n, nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false)
                .oriNode = n;
        
        // ---> ENSTM{EXPL_CONSTR}
        OldNodeSequenceVisitingProcessing.addENStmNode(NodeSequenceConstant.EXPL_CONSTR, nodeSequenceList);
    }
    
    // Eg: AppenderTable.class
    @Override
    public void visit(ClassExpr n, Object arg) {
        // Logger.log("ClassExpr:" + n + "\t\t" + n.getType() +"\t\t" +
        // n.getParentNode());
//        Node parentNode = n.getParentNode().orElse(null);
//
//        String varName = n.getType().asString() + ".class";
//        addVariableToScope(getAscendantTypeMethod(n), "Class", varName);
//
//        // AppenderTable.class ---> VAR(Class, AppenderTable.class)
//        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
//                curID, parentNode);
//        OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, varName, nodeSequenceStack, curMethodInfo, curTypeInfo,
//                nodeSequenceList);
//
//        curID++;

        // AppenderTable.class ---> LIT(Class)
        // Logger.log("ClassExpr:" + n + "\t\t" + n.getType() +"\t\t" +
        // n.getParentNode());
        String literalType = "Class";
        NodeInfo nodeInfo = NodeVisitProcessing.addNewLiteralNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        OldNodeSequenceVisitingProcessing.addLiteralNode(nodeInfo, literalType, nodeSequenceStack, curMethodInfo,
                curTypeInfo, nodeSequenceList, isParam);
        curID++;
    }
    
    // Eg: person.name
    @Override
    public void visit(FieldAccessExpr n, Object arg) {

        if (typeStack.size() > 0) {
            curTypeInfo = typeStack.peek();
        }
        // checkMethodCallExpr(n);

        NodeInfo nodeInfo = NodeVisitProcessing.addNewFieldAccessNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, n);
        curID++;

        String fieldName = n.getName().asString();
        int lastIdx = n.toString().lastIndexOf("." + fieldName);
        String varName = new String(n.toString().substring(0, lastIdx));
        varName = varName.replace("this", "this");
        varName = varName.replace("super", "super");

        doVisitExpression(n.getScope(), arg);
        // if (n.getScope() instanceof QualifiedNameExpr)
        // {
        // Logger.log("QualifiedNameExpr: " + n.getScope() + "\t" +
        // n.getScope().getClass());
        // }

        // person.name ---> F_ACCESS(Person, name)
        // this.name ---> F_ACCESS(null, name)
        OldNodeSequenceVisitingProcessing.addFieldAccessNode(nodeInfo, varName, fieldName, nodeSequenceStack,
                curMethodInfo, curTypeInfo, nodeSequenceList).oriNode = n;
    }
    
    // Eg: a.<String>move(x, y);
    @Override
    public void visit(MethodCallExpr n, Object arg) {
        curTypeInfo = typeStack.peek();
        fileInfo.numStatements++;

        //Node parentNode = n.getParentNode().orElse(null);
        //if (parentNode instanceof Parameter){
        //    Logger.log("MethodCallExpr: " +n + "\t" + n.getScope() +"\tparent: " +
        //            parentNode + "\tparentType:" + parentNode.getClass() );
        //}

        // If the method invocation has no nameExpr, the method is inner one
        String methodName = n.getName().asString();
        boolean isInner = true;
        String varName = "";
        if (!n.getScope().isPresent()) {
            // varName = curTypeInfo.typeName;
        } else {
            varName = n.getScope().get().toString();

            if (varName.equals("this")) {
                isInner = true;
            }
            // TODO: should make super better
            else if (varName.equals("super")) {
                isInner = false;
                // Change varType to its superclass
            } else if (varName.contains(".class")) {
                // Logger.log("classExpr: " + varName);
                isInner = false;
                addVariableToScope(getAscendantTypeMethod(n), "Class", varName.intern());
            } else {
                isInner = false;
            }
        }
        
        String attachedType = null;
        if (varName.equals("")) {
            varName = curTypeInfo.typeName;
            attachedType = curTypeInfo.typeName;
        }
        varName = varName.replace("this", "this");
        varName = varName.replace("super", "super");
        // Logger.logDebug("Args: " + n.getArgs() + "\t" + n.getTypeArgs());
        ArrayList<String> parameterList = null;
        if (n.getArguments() != null) {
            parameterList = new ArrayList<String>();
            for (Expression exp : n.getArguments()) {
                parameterList.add(exp.toString());
            }
        }

        MethodInvocInfo methodInvocInfo = new MethodInvocInfo(isInner, methodName, varName, parameterList);

        if (curMethodInfo != null) {
            if (curMethodInfo.methodInvocList == null) {
                MethodInfo.methodInvocTmpList = new ArrayList<MethodInvocInfo>(1);
            }
            MethodInfo.methodInvocTmpList.add(methodInvocInfo);
        } else {
            curTypeInfo = typeStack.peek();
            if (curTypeInfo.methodInvocList == null) {
                curTypeInfo.methodInvocList = new ArrayList<MethodInvocInfo>(1);
            }
            curTypeInfo.methodInvocList.add(methodInvocInfo);
        }

        NodeInfo nodeInfo = NodeVisitProcessing.addNewInvocNode(curMethodInfo, parentNodeStack,
                previousControlFlowNodeStack, curID, methodInvocInfo, n);

        curID++;
        curNode = nodeInfo;

        // a --->
        if (n.getScope().isPresent()) {
            doVisitExpression(n.getScope().get(), arg);
        }

        int numOfArgs = 0;
        if (n.getArguments() != null) {
            numOfArgs = n.getArguments().size();
            //System.out.println(n.getName() + "-" + numOfArgs);
            if (n.getArguments().isNonEmpty()) {
                for (int i = 0; i < n.getArguments().size(); i++) {
                    Expression methodArg = n.getArgument(i);
                    if (methodArg instanceof NameExpr) {
                        //NameExpr v = (NameExpr) methodArg;
                    }
                    //System.out.println(methodArg.getClass());
                }
            }
            //System.out.println("---------------");
        }

        // move ---> M_ACCESS([TYPE OF a],move,2)
        OldNodeSequenceVisitingProcessing.addMethodAccessNode(nodeInfo, varName, attachedType, methodName,
                nodeSequenceStack, curMethodInfo, curTypeInfo, nodeSequenceList, numOfArgs).oriNode = n;

        // ( ---> OPEN_PART
        // Start arguments
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
        
        // x, y --->
        visitExpressionList(n.getArguments(), arg);
        NodeVisitProcessing.removeInvocNodeInfo(n, nodeInfo, parentNodeStack, previousControlFlowNodeStack);
        
        // ) ---> CLOSE_PART
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false)
                .oriNode = n;
        // Start arguments
    }

    //TODO: make its tokens different from method call's
    // Eg: getNames()[15*15]
    @Override
    public void visit(ArrayAccessExpr n, Object arg) {
        // System.out.println(n.getName().getClass() + "-" + n.getIndex().getClass());
        
        // getNames() --->
        doVisitExpression(n.getName(), arg);
        
        // [ ---> OPEN_BRAK
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.ARRAY_ACCESS, nodeSequenceList, true);
        
        // 15*15 --->
        doVisitExpression(n.getIndex(), arg);
        
        // ] ---> CLOSE_BRAK
        OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.ARRAY_ACCESS, nodeSequenceList, false);
    }
    
    // Eg: A(1, 2) {...} """in""" enum X { A(1, 2) {...}, B(3, 4) {...}}
    @Override
    public void visit(EnumConstantDeclaration n, Object arg) {
        fileInfo.numStatements++;
        
        if (typeStack.size() > 0) {
            curTypeInfo = typeStack.peek();
        }
        if (methodInfoStack.size() > 0) {
            curMethodInfo = methodInfoStack.peek();
        }

        TypeInfo typeInfo = new TypeInfo();
        typeInfo.typeName = n.getName().asString();
        typeInfo.packageDec = fileInfo.packageDec;

        fileInfo.typeInfoList.add(typeInfo);

        typeInfo.fileInfo = fileInfo;

        Node ascendant = getAscendantTypeMethod(n);
        if ((ascendant instanceof ClassOrInterfaceDeclaration) || (ascendant instanceof EnumDeclaration)
                || (ascendant instanceof AnnotationDeclaration)) {
            typeInfo.parentInfo = curTypeInfo;
        } else if ((ascendant instanceof MethodDeclaration) || (ascendant instanceof ConstructorDeclaration)) {
            typeInfo.parentInfo = curMethodInfo;
        }

        curTypeInfo = typeInfo;

        typeStack.push(typeInfo);
        
        // Add this variable for current class
        addVariableToScope(n, typeInfo.typeName.intern(), "this");
        addVariableToScope(n, typeInfo.typeName.intern(), typeInfo.typeName.intern() + ".this");

        // A ---> CLASS{START,A}
        NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addClassNode(typeInfo.typeName.intern(),
                nodeSequenceStack, typeInfo, nodeSequenceList);
        nodeSequenceInfo.oriNode = n;
        
        if (n.getArguments().isNonEmpty()) {
            // ( ---> OPEN_PART
            OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, true);
            
            // 1, 2 --->
            visitExpressionList(n.getArguments(), arg);
            
            // ) ---> CLOSE_PART
            OldNodeSequenceVisitingProcessing.addPartNode(NodeSequenceConstant.NODE_PART, nodeSequenceList, false);
        }
        
        if (n.getClassBody().isNonEmpty()) {
            // {...} --->
            visitClassBody(n.getClassBody(), arg);
        }
        
        // ---> CLASS{END,A}
        OldNodeSequenceVisitingProcessing.addEndClassNode(typeInfo.typeName.intern(), nodeSequenceInfo.nodeSeqID,
                nodeSequenceStack, typeInfo, nodeSequenceList);
        
        curMethodInfo = null;
        typeStack.pop();
    }
    
    // Eg: int id() default 1; """in""" @interface X { int id() default 1; }
    @Override
    public void visit(AnnotationMemberDeclaration n, Object arg) {      
        // int --->
        doVisitType(n.getType(), arg);
        
        // id ---> VAR(int, id)
        NodeInfo nodeInfo = NodeVisitProcessing.addVarNode(curMethodInfo, parentNodeStack, previousControlFlowNodeStack,
                curID, n);
        String varName = n.getName().asString();
        NodeSequenceInfo nodeSequenceInfo = OldNodeSequenceVisitingProcessing.addVarNode(nodeInfo, varName, nodeSequenceStack, curMethodInfo, curTypeInfo,
                nodeSequenceList);
        curID++;
        
        nodeSequenceInfo.setAttachedType(n.getTypeAsString());
        
        Expression defaultVal = n.getDefaultValue().orElse(null);

        // default ---> ASSIGN(ASSIGN)
        if (defaultVal != null) {
            String assignType = Operator.ASSIGN.toString();
            NodeInfo assignNodeInfo = NodeVisitProcessing.addNewAssignNode(assignType, curMethodInfo, parentNodeStack,
                    previousControlFlowNodeStack, curID, n);

            OldNodeSequenceVisitingProcessing.addAssignmentNode(assignNodeInfo, assignType, nodeSequenceStack, curMethodInfo,
                    curTypeInfo, nodeSequenceList);
            curID++;
        }

        // 1 --->
        if (defaultVal != null) {
            doVisitExpression(defaultVal, arg);
        }
    }
    
    // Eg: a=15 """in""" @Counters(a=15)
    @Override
    public void visit(MemberValuePair n, Object arg) {
        Logger.log("MemberValuePair: " + n);
    }
	
	// TODO: handle this
	// Eg: (a, b) -> { println(a + b); }
    @Override
    public void visit(LambdaExpr n, Object arg) {
        //super.visit(n, arg);
        OldNodeSequenceVisitingProcessing.addLambdaExprNode(nodeSequenceList).oriNode = n;
    }
    
    // TODO: handle Lambda expressions and this
    // Eg: x """in""" d = x -> (int)x + 1;
    @Override
    public void visit(UnknownType n, Object arg) {
    }
	
	// TODO: handle Lambda expressions and this
	// Eg: Bar<String>::<Integer>new
    @Override
    public void visit(MethodReferenceExpr n, Object arg) {
        //super.visit(n, arg);
    }
	
	// TODO: handle Lambda expressions and this
	// Eg: World """in""" World::greet
    @Override
    public void visit(TypeExpr n, Object arg) {
        //super.visit(n, arg);
    }
    
    @Override
    public void visit(UnparsableStmt n, Object arg) {
        Logger.log("Unparsable Statement: " + n.toString());
    }
    
    // Eg: /* a comment */
    @Override
    public void visit(BlockComment n, Object arg) {
        // fileInfo.numBlockComments++;
    }
    
    // Eg: /** a comment */
    @Override
    public void visit(JavadocComment n, Object arg) {
        fileInfo.numJavadocs++;
    }
    
    // Eg: //Some comment\n
    @Override
    public void visit(LineComment n, Object arg) {
        fileInfo.numLineComments++;
    }
	
	public void doVisit(Node n, Object arg) {
	    if (n instanceof Statement) {
            // if(n.toString().equals("activateNow")) {
            // System.out.println("3"+n.getClass());
            // }
            doVisitStatement((Statement) n, arg);
        } else if (n instanceof Expression) {
            doVisitExpression((Expression) n, arg);
        } else if (n instanceof Type) {
            doVisitType((Type) n, arg);
        } else {
            if (n instanceof Parameter) {
                // getParameter((Parameter) n);
                visit((Parameter) n, arg);
            } else if (n instanceof SwitchEntry) {
                visit((SwitchEntry) n, arg);
            } else {
                Logger.log("Not found Node: " + n + "\t" + n.getClass());
            }
        }
    }
	
	public void doVisitStatement(Statement n, Object arg) {
        if (n instanceof AssertStmt) {
            visit((AssertStmt) n, arg);
        } else if (n instanceof BlockStmt) {
            visit((BlockStmt) n, arg);
        } else if (n instanceof BreakStmt) {
            visit((BreakStmt) n, arg);
        } else if (n instanceof ContinueStmt) {
            visit((ContinueStmt) n, arg);
        } else if (n instanceof DoStmt) {
            visit((DoStmt) n, arg);
        } else if (n instanceof EmptyStmt) {
            visit((EmptyStmt) n, arg);
        } else if (n instanceof ExplicitConstructorInvocationStmt) {
            visit((ExplicitConstructorInvocationStmt) n, arg);
        } else if (n instanceof ExpressionStmt) {
            visit((ExpressionStmt) n, arg);
        } else if (n instanceof ForEachStmt) {
            visit((ForEachStmt) n, arg);
        } else if (n instanceof ForStmt) {
            visit((ForStmt) n, arg);
        } else if (n instanceof IfStmt) {
            visit((IfStmt) n, arg);
        } else if (n instanceof LabeledStmt) {
            visit((LabeledStmt) n, arg);
        } else if (n instanceof LocalClassDeclarationStmt) {
            visit((LocalClassDeclarationStmt) n, arg);
        } else if (n instanceof ReturnStmt) {
            visit((ReturnStmt) n, arg);
        } else if (n instanceof SwitchStmt) {
            visit((SwitchStmt) n, arg);
        } else if (n instanceof SynchronizedStmt) {
            visit((SynchronizedStmt) n, arg);
        } else if (n instanceof ThrowStmt) {
            visit((ThrowStmt) n, arg);
        } else if (n instanceof TryStmt) {
            visit((TryStmt) n, arg);
        } else if (n instanceof WhileStmt) {
            visit((WhileStmt) n, arg);
        } else if (n instanceof YieldStmt) {
            visit((YieldStmt) n, arg);
        } else {
            Logger.log("Not found stmt: " + n + "\t" + n.getClass());
        }
    }

    public void doVisitExpression(Expression n, Object arg) {
        if (n instanceof MarkerAnnotationExpr) {
            visit((MarkerAnnotationExpr) n, arg);
        } else if (n instanceof NormalAnnotationExpr) {
            visit((NormalAnnotationExpr) n, arg);
        } else if (n instanceof SingleMemberAnnotationExpr) {
            visit((SingleMemberAnnotationExpr) n, arg);
        } else if (n instanceof ArrayAccessExpr) {
            visit((ArrayAccessExpr) n, arg);
        } else if (n instanceof ArrayCreationExpr) {
            visit((ArrayCreationExpr) n, arg);
        } else if (n instanceof ArrayInitializerExpr) {
            visit((ArrayInitializerExpr) n, arg);
        } else if (n instanceof AssignExpr) {
            visit((AssignExpr) n, arg);
        } else if (n instanceof BinaryExpr) {
            visit((BinaryExpr) n, arg);
        } else if (n instanceof CastExpr) {
            visit((CastExpr) n, arg);
        } else if (n instanceof ClassExpr) {
            visit((ClassExpr) n, arg);
        } else if (n instanceof ConditionalExpr) {
            visit((ConditionalExpr) n, arg);
        } else if (n instanceof EnclosedExpr) {
            visit((EnclosedExpr) n, arg);
        } else if (n instanceof FieldAccessExpr) {
            visit((FieldAccessExpr) n, arg);
        } else if (n instanceof InstanceOfExpr) {
            visit((InstanceOfExpr) n, arg);
        } else if (n instanceof LambdaExpr) {
            visit((LambdaExpr) n, arg);
        } else if (n instanceof BooleanLiteralExpr) {
            visit((BooleanLiteralExpr) n, arg);
        } else if (n instanceof CharLiteralExpr) {
            visit((CharLiteralExpr) n, arg);
        } else if (n instanceof DoubleLiteralExpr) {
            visit((DoubleLiteralExpr) n, arg);
        } else if (n instanceof IntegerLiteralExpr) {
            visit((IntegerLiteralExpr) n, arg);
//      } else if (n instanceof IntegerLiteralMinValueExpr) {
//          visit((IntegerLiteralMinValueExpr) n, arg);
        } else if (n instanceof LongLiteralExpr) {
            visit((LongLiteralExpr) n, arg);
//      } else if (n instanceof LongLiteralMinValueExpr) {
//          visit((LongLiteralMinValueExpr) n, arg);
        } else if (n instanceof StringLiteralExpr) {
            visit((StringLiteralExpr) n, arg);
        } else if (n instanceof TextBlockLiteralExpr) {
            visit((TextBlockLiteralExpr) n, arg);
        } else if (n instanceof NullLiteralExpr) {
            visit((NullLiteralExpr) n, arg);
        } else if (n instanceof MethodCallExpr) {
            visit((MethodCallExpr) n, arg);
        } else if (n instanceof MethodReferenceExpr) {
            visit((MethodReferenceExpr) n, arg);
        } else if (n instanceof NameExpr) {
            visit((NameExpr) n, arg);
        } else if (n instanceof ObjectCreationExpr) {
            visit((ObjectCreationExpr) n, arg);
        } else if (n instanceof SuperExpr) {
            visit((SuperExpr) n, arg);
        } else if (n instanceof ThisExpr) {
            visit((ThisExpr) n, arg);
        } else if (n instanceof TypeExpr) {
            visit((TypeExpr) n, arg);
        } else if (n instanceof UnaryExpr) {
            visit((UnaryExpr) n, arg);
        } else if (n instanceof VariableDeclarationExpr) {
            visit((VariableDeclarationExpr) n, arg);
        } else {
            Logger.log("Not found Expression: " + n + "\t" + n.getClass());
        }
    }
    
    public void doVisitType(Type n, Object arg) {
        if (n instanceof IntersectionType) {
            visit((IntersectionType) n, arg);
        } else if (n instanceof PrimitiveType) {
            visit((PrimitiveType) n, arg);
        } else if (n instanceof ArrayType) {
            visit((ArrayType) n, arg);
        } else if (n instanceof ClassOrInterfaceType) {
            visit((ClassOrInterfaceType) n, arg);
        } else if (n instanceof TypeParameter) {
            visit((TypeParameter) n, arg);
        } else if (n instanceof UnionType) {
            visit((UnionType) n, arg);
        } else if (n instanceof UnknownType) {
            visit((UnknownType) n, arg);
        } else if (n instanceof VarType) {
            visit((VarType) n, arg);
        } else if (n instanceof VoidType) {
            visit((VoidType) n, arg);
        } else if (n instanceof WildcardType) {
            visit((WildcardType) n, arg);
        } else {
            Logger.log("Not found type: " + n + "\t" + n.getClass());
        }
    }
    
    public Node getAscendantType(Node n) {
        Node parentNode = n.getParentNode().orElse(null);
        while ((parentNode != null) && (!(parentNode instanceof CompilationUnit))
                && (!(parentNode instanceof ClassOrInterfaceDeclaration)) && (!(parentNode instanceof EnumDeclaration))
                && (!(parentNode instanceof AnnotationDeclaration))
                ) {
            // Logger.log("parentNode class: " + parentNode.getClass());
            parentNode = parentNode.getParentNode().orElse(null);
        }
        return parentNode;
    }
	
	public Node getAscendantTypeMethod(Node n) {
        Node parentNode = n.getParentNode().orElse(null);
        while ((parentNode != null) && (!(parentNode instanceof CompilationUnit))
                && (!(parentNode instanceof ClassOrInterfaceDeclaration)) && (!(parentNode instanceof EnumDeclaration))
                && (!(parentNode instanceof AnnotationDeclaration))
                && (!(parentNode instanceof MethodDeclaration)) && (!(parentNode instanceof ConstructorDeclaration))
                ) {
            // Logger.log("parentNode class: " + parentNode.getClass());
            parentNode = parentNode.getParentNode().orElse(null);
        }
        return parentNode;
    }
	
	/**
     * Gets name and type of the given parameter
     * adds them to {@link MethodInfo#shortScopeVariableMap} of {@link #curMethodInfo}
     * or {@link TypeInfo#shortScopeVariableMap} of {@link #curTypeInfo}.
     */
    public void getParameter(Parameter n) {
        Node parentNode = getAscendantTypeMethod(n);
        //Logger.log("VariableDeclaration: " + n + "\tparent: " +
        //parentNode.getClass());

        Type varType = n.getType();

        //Logger.logDebug("VariableDeclaration: " + n.getName().asString().intern() + "\tType: " +
        //        varType.toString().intern() + "\tparent: " + parentNode.getClass() );
        //Logger.log("VariableDeclaration: " + n.getName().asString().intern() + "\tType: " +
        //        varType.toString().intern() + "\tparent: " + parentNode.getClass() );
        //Logger.log("parent: " + parentNode);
        String varTypeName = varType.asString().intern();
        if (varTypeName.indexOf('.') >= 0) {
            varTypeName = varTypeName.substring(varTypeName.lastIndexOf('.') + 1);
        }
        addVariableToScope(parentNode, varTypeName + (n.isVarArgs()? "[]": ""),
                    n.getName().asString().intern());
    }

    // public void checkMethodCallExpr(Node n){
    // Node parentNode = n.getParentNode();
    // if (parentNode instanceof MethodCallExpr){
    // String parentNodeStr = parentNode.toString();
    // if (parentNodeStr.startsWith(n.toString()+"."))
    // {
    // // Logger.logDebug("\tMethodCall's Expr: " + n);
    // }
    // }
    // }

	/**
	 * Gets variable's name and type and
	 * adds them to {@link MethodInfo#shortScopeVariableMap} of {@link #curMethodInfo}
	 * or {@link TypeInfo#shortScopeVariableMap} of {@link #curTypeInfo}.
	 */
	public void getVariable(VariableDeclarator n) {
		Node parentNode = n.getParentNode().orElse(null);
		// Logger.logDebug("VariableDeclaration: " + n + "\tparent: " +
		// parentNode.getClass());

		Type varType = null;

		while (!(parentNode instanceof ClassOrInterfaceDeclaration) && !(parentNode instanceof EnumDeclaration)
				&& !(parentNode instanceof AnnotationDeclaration)
				&& !(parentNode instanceof MethodDeclaration) && !(parentNode instanceof ConstructorDeclaration)) {
			if (parentNode instanceof FieldDeclaration) {
				varType = n.getType();
			} else if (parentNode instanceof VariableDeclarationExpr) {
				varType = n.getType();
			}

			if (parentNode == null) {
				Logger.log("node: " + n);
				Logger.log("parentNode: " + parentNode + "\t" + curTypeInfo + "\t" + fileInfo.filePath);
			}

			parentNode = parentNode.getParentNode().orElse(null);
		}

		// Logger.logDebug("VariableDeclaration: " + n.getId().getName() + "\tType: " +
		// varType + "\tparent: " + parentNode.getClass() );
        String varTypeName = varType.asString().intern();
		if (varTypeName.indexOf('.') >= 0) {
		    varTypeName = varTypeName.substring(varTypeName.lastIndexOf('.') + 1);
        }
		addVariableToScope(parentNode, varTypeName, n.getName().asString().intern());
	}
	
	/**
     * Adds variable to {@link MethodInfo#shortScopeVariableMap} of {@link #curMethodInfo}
     * or {@link TypeInfo#shortScopeVariableMap} of {@link #curTypeInfo}.
     */
	public void addVariableToScope(Node n, String varType, String varName) {
	    if (methodInfoStack.size() > 0) {
            curMethodInfo = methodInfoStack.peek();
        }
        curTypeInfo = typeStack.peek();

        if ((n instanceof MethodDeclaration) || (n instanceof ConstructorDeclaration)) {
            if (curMethodInfo == null) {
                Logger.log("node: " + n);
                Logger.log("parentNode: " + n + "\t" + curTypeInfo);
            }
            if (curMethodInfo.shortScopeVariableMap == null) {
                curMethodInfo.shortScopeVariableMap = new HashMap<String, String>(1);
            }
            curMethodInfo.shortScopeVariableMap.put(varName, varType);
        } else if ((n instanceof ClassOrInterfaceDeclaration) || (n instanceof EnumDeclaration)
                || (n instanceof AnnotationDeclaration)) {
            if (curTypeInfo.shortScopeVariableMap == null) {
                curTypeInfo.shortScopeVariableMap = new HashMap<String, String>(1, 0.9f);
            }
            curTypeInfo.shortScopeVariableMap.put(varName, varType);
        }
	}

	private static String getStringWithoutComment(Node n) {
        String content = n.toString();
        if (n.getComment().isPresent()) {
            content = content.substring(n.getComment().get().toString().length());
        }
        return content;
    }

	private static Optional<Position> getPositionFrom(Optional<Position> origin, int lineDif, int colDif) {
	    if (!origin.isPresent()) return Optional.empty();
	    Position pos = origin.get();
	    return Optional.of(new Position(pos.line + lineDif, pos.column + colDif));
    }

    private static Optional<Position> getPositionFrom(Optional<Position> origin, int colDif) {
	    return getPositionFrom(origin, 0, colDif);
    }

    private static Optional<Position> getPositionOfStringFrom(Optional<Position> origin, String s, String t) {
        if (!origin.isPresent()) return Optional.empty();
        Position pos = origin.get();
	    if (s.indexOf(t) < 0) return Optional.empty();
	    s = s.substring(0, s.indexOf(t) + 1);
	    int lineCnt = 0;
	    while (s.indexOf('\n') >= 0) {
	        ++lineCnt;
	        s = s.substring(s.indexOf('\n') + 1);
        }
	    if (lineCnt == 0) {
            return Optional.of(new Position(pos.line, pos.column + s.length() - 1));
        }
	    else return Optional.of(new Position(pos.line + lineCnt, s.length()));
    }

    private static Optional<Position> getLastPositionOfStringFrom(Optional<Position> origin, String s, String t) {
        if (!origin.isPresent()) return Optional.empty();
        Position pos = origin.get();
        if (s.lastIndexOf(t) < 0) return Optional.empty();
        s = s.substring(0, s.lastIndexOf(t) + 1);
        int lineCnt = 0;
        while (s.indexOf('\n') >= 0) {
            ++lineCnt;
            s = s.substring(s.indexOf('\n') + 1);
        }
        if (lineCnt == 0) {
            return Optional.of(new Position(pos.line, pos.column + s.length() - 1));
        }
        else return Optional.of(new Position(pos.line + lineCnt, s.length()));
    }
	
	public synchronized static void main(String[] args) {
        Logger.initDebug("debugVisitor.txt");
        List<File> allSubFilesTmp = DirProcessor.walkJavaFile(Config.REPO_DIR + "sampleproj/");
        Logger.log("allSubFiles size: " + allSubFilesTmp.size());
        MetricsVisitor visitor = new MetricsVisitor();
        SystemTableCrossProject systemTableCrossProject = new SystemTableCrossProject();
        for (File file : allSubFilesTmp) {
            JavaFileParser.visitFile(visitor, file, systemTableCrossProject, "../xxx/");
        }
        GetDirStructureCrossProject.buildSystemPackageList(systemTableCrossProject);
        systemTableCrossProject.buildTypeFullMap();
        systemTableCrossProject.buildMethodMap();
        systemTableCrossProject.buildMethodFullMap();
        systemTableCrossProject.buildFeasibleTypeListForFiles();
        systemTableCrossProject.buildTypeFullVariableMap();
        systemTableCrossProject.buildMethodInvocListForMethods();
        systemTableCrossProject.buildMethodDics();
        systemTableCrossProject.getTypeVarNodeSequence();
        systemTableCrossProject.buildNodeSeqDic();
        systemTableCrossProject.buildMethodInvocListForFiles();
        Logger.closeDebug();
    }
}
