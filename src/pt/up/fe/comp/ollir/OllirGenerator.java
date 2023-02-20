package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.SymbolTableBuilder;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;
import static pt.up.fe.comp.ast.AstNode.*;
import static pt.up.fe.comp.ollir.OllirExprGenerator.newVar;
import static pt.up.fe.comp.ollir.OllirExprGenerator.newVarInstr;
import static pt.up.fe.comp.ollir.OllirUtils.*;


public class OllirGenerator extends AJmmVisitor<Integer, Integer> {

    private final StringBuilder code;
    private final SymbolTableBuilder symbolTable;
    private final OllirExprGenerator ollirExprGenerator;
    public static int identention = 0;
    private int whileNumber;
    private int ifNumber;

    public OllirGenerator(SymbolTableBuilder symbolTable) {
        this.ollirExprGenerator = new OllirExprGenerator(symbolTable);
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.whileNumber = 0;
        this.ifNumber = 0;

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECLARATION, this::visitClassDeclaration);
        addVisit(METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(RETURN_STATEMENT, this::visitReturnStatement);

        addVisit(CONDITION, this::visitCondition);

        addVisit(EXPRESSION_STATEMENT, this::visitExpressionStatement);
        addVisit(IF_STATEMENT, this::visitIfStatement);
        addVisit(WHILE_STATEMENT, this::visitWhileStatement);
        addVisit(ASSIGNMENT_STATEMENT, this::visitAssignmentStatement);
        addVisit(SCOPE, this::visitScope);
    }

    public String getCode() {
        return this.code.toString();
    }

    private Integer visitProgram(JmmNode program, Integer dummy) {
        for (var importStr : symbolTable.getImports())  {
            code.append("import ").append(importStr).append(";\n");
        }

        for (var child : program.getChildren()) {
            visit(child);
        }

        return 0;
    }

    private Integer visitClassDeclaration(JmmNode classDeclaration, Integer dummy) {
        code.append("public ").append(symbolTable.getClassName());
        String extendedClass = symbolTable.getSuper();
        if (extendedClass != null) {
            code.append(" extends ").append(extendedClass);
        }

        code.append(" {\n");
        identention++;

        buildPrivateAttibutes();
        buildConstructor();

        for (var child : classDeclaration.getChildren()) {
            if (child.getKind().equals(METHOD_DECLARATION.toString()))
                visit(child);
        }
        identention--;
        code.append("}\n");

        return 0;
    }

    private void buildPrivateAttibutes() {
        for(Symbol field : symbolTable.getFields()){
            code.append(ident()).append(".field private ")
                    .append(OllirUtils.getCode(field))
                    .append(";\n");
        }
    }
    private void buildConstructor() {
        code.append(ident()).append(".construct ").append(symbolTable.getClassName()).append("().V {\n");
        identention++;
        code.append(ident()).append("invokespecial(this, \"<init>\").V;\n");
        identention--;
        code.append(ident()).append("}\n");
    }
    
    private Integer visitMethodDeclaration(JmmNode methodDeclaration, Integer dummy) {
        OllirExprGenerator.varAuxNumber = 0;
        String methodSignature = methodDeclaration.get("name");
        boolean isStatic = Boolean.parseBoolean(methodDeclaration.get("static"));
        code.append(ident()).append(".method public ");
        if (isStatic) code.append("static ");
        code.append(methodSignature).append("(");

        List<Symbol> params = symbolTable.getParameters(methodSignature);
        String paramCode = params.stream().map(symbol -> OllirUtils.getCode(symbol)).collect(Collectors.joining(", "));

        code.append(paramCode);
        code.append(").");
        code.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));
        code.append(" {\n");
        identention++;


        int firstExpressionIndex = 0;
        while (firstExpressionIndex < methodDeclaration.getNumChildren() &&
                (methodDeclaration.getJmmChild(firstExpressionIndex).getKind().equals(METHOD_PARAMETERS.toString())
                || methodDeclaration.getJmmChild(firstExpressionIndex).getKind().equals(VAR_DECLARATION.toString()))) {
            firstExpressionIndex++;
        }

        List<JmmNode> stmts = methodDeclaration.getChildren().subList(firstExpressionIndex, methodDeclaration.getNumChildren());
        for (JmmNode stmt : stmts) {
            visit(stmt);
        }

        if(this.symbolTable.getReturnType(methodSignature).getName().equals("void")) {
            code.append(ident()).append("ret.V;\n");
        }

        identention--;
        code.append(ident()).append("}\n");

        return 0;
    }

    private Integer visitReturnStatement(JmmNode returnStatement, Integer dummy) {
        OllirExprCode result = visitExpression(returnStatement.getJmmChild(0));

        code.append(result.getTemps());
        String t1 = result.getFullExp();
        if (isComplex(t1)) {
            t1 = newVar(result.getType());
            code.append(ident()).append(newVarInstr(t1, result.getType(), result.getFullExp()));
        }
        code.append(ident()).append("ret.").append(result.getType());
        code.append(" ").append(t1);
        code.append(";\n");
        
        return 0;
    }

    private Integer visitExpressionStatement(JmmNode expressionStatement, Integer dummy) {
        OllirExprCode exprPair = this.visitExpression(expressionStatement.getJmmChild(0));
        code.append(exprPair.getTemps());
        code.append(ident()).append(exprPair.getFullExp()).append(";\n");
        return 0;
    }

    private Integer visitIfStatement(JmmNode ifStatement, Integer dummy) {
        JmmNode condition = ifStatement.getJmmChild(0);
        JmmNode ifBlock = ifStatement.getJmmChild(1);
        JmmNode elseBlock = ifStatement.getJmmChild(2);

        visit(condition);
        int thisIfNumber = ifNumber++;
        for(JmmNode elseChild : elseBlock.getChildren()){
            visit(elseChild);
        }

        code.append(ident()).append("goto EndIf").append(thisIfNumber).append(";\n");
        code.append(ident(true)).append("IfBlock").append(thisIfNumber).append(": \n");

        for(JmmNode ifChild : ifBlock.getChildren()){
            visit(ifChild);
        }


        code.append(ident(true)).append("EndIf").append(thisIfNumber).append(":\n");
        return 0;
    }

    private Integer visitWhileStatement(JmmNode whileStatement, Integer dummy) {
        JmmNode condition = whileStatement.getJmmChild(0);
        JmmNode whileBlock = whileStatement.getChildren().get(1);

        code.append(ident(true)).append("Loop").append(whileNumber).append(":\n");

        visit(condition);
        int thisWhileNumber = whileNumber++;

        code.append(ident(true)).append("Body").append(thisWhileNumber).append(":\n");
        visit(whileBlock.getJmmChild(0));
        code.append(ident()).append("goto Loop").append(thisWhileNumber).append(";\n");

        code.append(ident(true)).append("EndLoop").append(thisWhileNumber).append(":\n");
        return 0;
    }

    private Integer visitAssignmentStatement(JmmNode assignmentStatement, Integer dummy) {
        OllirExprCode left = visitExpression(assignmentStatement.getJmmChild(0));
        OllirExprCode right = visitExpression(assignmentStatement.getJmmChild(1));

        code.append(left.getTemps());
        code.append(right.getTemps());
        var leftFullExp = left.getFullExp();
        if (isGetfield(leftFullExp)) {
            String aux = leftFullExp.substring(leftFullExp.indexOf(",")+1, leftFullExp.indexOf(")"));
            String t2 = right.getFullExp();
            if (isComplex(t2) || isParam(t2)) {
                t2 = newVar(right.getType());
                code.append(ident()).append(newVarInstr(t2, right.getType(), right.getFullExp()));
            }
            code.append(ident()).append("putfield(this,").append(aux).append(", ").append(t2).append(").V;\n");
        }
        else {
            String rightFullExp = right.getFullExp();
            if (isInvoke(rightFullExp)) {
                rightFullExp = rightFullExp.substring(0, rightFullExp.lastIndexOf('.') + 1).concat(left.getType());
            }

            code.append(ident())
                    .append(left.getFullExp())
                    .append(" :=.").append(left.getType())
                    .append(" ").append(rightFullExp).append(";\n");
        }
        return 0;
    }

    private Integer visitScope(JmmNode scope, Integer dummy) {
        for(JmmNode scopeChild : scope.getChildren()) {
            visit(scopeChild);
        }

        return 0;
    }

    private Integer visitCondition(JmmNode condition, Integer dummy) {
        StringBuilder temps = new StringBuilder();
        OllirExprCode cond = visitExpression(condition.getJmmChild(0));

        temps.append(cond.getTemps());
        String t1 = cond.getFullExp();
        if (isComplex(t1) && !isArithExpr(t1)) {
            t1 = newVar(cond.getType());
            temps.append(ident()).append(newVarInstr(t1, cond.getType(), cond.getFullExp()));
        }

        switch (condition.getJmmParent().getKind()) {
            case "IfStatement":
                code.append(temps);
                code.append(ident()).append("if (").append(t1).append(") goto IfBlock").append(ifNumber).append(";\n");
                break;
            case "WhileStatement":
                code.append(temps);
                code.append(ident()).append("if (").append(t1).append(") goto Body").append(whileNumber).append(";\n")
                    .append(ident()).append("goto EndLoop").append(whileNumber).append(";\n");
                break;
            default:
                break;
        }

        return 0;
    }

    private OllirExprCode visitExpression(JmmNode expression) {
        return this.ollirExprGenerator.visit(expression);
    }

    static String ident() {
        return "\t".repeat(Math.max(0, identention));
    }
    static String ident(boolean label) {
        return "\t".repeat(Math.max(0, identention - 1)) + "  ";
    }

}
