package pt.up.fe.comp.analysis.analysers;

import pt.up.fe.comp.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp.analysis.AnalysisUtils.*;
import static pt.up.fe.comp.ast.AstNode.*;

public class CheckImportsVisitor extends SemanticAnalyserVisitor {
    public CheckImportsVisitor() {
        super();
        addVisit(CLASS_DECLARATION, this::visitClassDeclarationn);
        addVisit(NEW_OBJECT, this::visitNewObject);
        addVisit(VAR_DECLARATION, this::visitVarDeclaration);
        addVisit(METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(DOT_EXP, this::visitDotExp);
    }

    private Boolean visitDotExp(JmmNode dotExp, SymbolTableBuilder symbolTableBuilder) {
        JmmNode leftNode = dotExp.getJmmChild(0);
        if (leftNode.getKind().equals(IDENTIFIER_LITERAL.toString())) {
            if (isIdentifierDeclared(leftNode, symbolTableBuilder))
                return true;
            if (leftNode.get("val").equals(symbolTableBuilder.getClassName()))
                return true;
            if (isImported(leftNode.get("val"), symbolTableBuilder))
                return true;
            addReport(leftNode, "Identifier " + leftNode.get("val") + " not found");
            return false;
        }
        return true;
    }

    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, SymbolTableBuilder symbolTableBuilder) {
        String type = methodDeclaration.get("type");
        if (type.equals(symbolTableBuilder.getClassName()) || isBuiltInType(type) || isImported(type, symbolTableBuilder))
            return true;
        addReport(methodDeclaration, "Return type "+type+" not found");
        return false;
    }

    private Boolean visitVarDeclaration(JmmNode varDeclaration, SymbolTableBuilder symbolTableBuilder) {
        var varType = varDeclaration.get("type");
        if (isBuiltInType(varType))
            return true;
        if (varType.equals(symbolTableBuilder.getClassName()))
            return true;
        if (isImported(varType, symbolTableBuilder))
            return true;
        addReport(varDeclaration, "Type "+varType+" of variable "+varDeclaration.get("var")+" not imported");
        return false;
    }

    private Boolean visitClassDeclarationn(JmmNode classDeclaration, SymbolTableBuilder symbolTableBuilder) {
        var superClassOpt = classDeclaration.getOptional("extends");
        if (superClassOpt.isPresent()) {
            String superClass = superClassOpt.get();
            if (isImported(superClass, symbolTableBuilder))
                return true;
            addReport(classDeclaration, "Super class "+superClass+" was not imported");
            return false;
        }
        return true;
    }

    private Boolean visitNewObject(JmmNode newObject, SymbolTableBuilder symbolTableBuilder) {
        String objectName = newObject.get("name");
        if (objectName.equals(symbolTableBuilder.getClassName()) || isImported(objectName, symbolTableBuilder))
            return true;
        addReport(newObject, "Object "+objectName + " not found");
        return false;
    }
}
