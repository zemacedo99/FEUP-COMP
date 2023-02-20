package pt.up.fe.comp.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import static pt.up.fe.comp.ast.AstNode.*;

public class DeadCodeEliminationVisitor extends PreorderJmmVisitor<String, Boolean> {
    private int counter;

    public DeadCodeEliminationVisitor() {
        this.counter = 0;

        addVisit(IF_STATEMENT, this::visitIfStatement);
        addVisit(WHILE_STATEMENT, this::visitWhileStatement);
    }

    Boolean getBoolean(JmmNode jmmNode) {
        if (jmmNode.getKind().equals(TRUE_LITERAL.toString())) return true;
        if (jmmNode.getKind().equals(FALSE_LITERAL.toString())) return false;
        return null;
    }

    private Boolean visitIfStatement(JmmNode jmmNode, String s) {
        var condition = getBoolean(jmmNode.getJmmChild(0).getJmmChild(0));
        if (condition == null) return false;

        if (condition) {
            JmmNode ifBlock = jmmNode.getJmmChild(1).getJmmChild(0);

            JmmNode newNode = new JmmNodeImpl(ifBlock.getKind());
            newNode.put("col", jmmNode.get("col"));
            newNode.put("line", jmmNode.get("line"));
            for (JmmNode child : ifBlock.getChildren())
                newNode.add(child);

            jmmNode.replace(newNode);
        }
        else {
            JmmNode elseBlock = jmmNode.getJmmChild(2).getJmmChild(0);

            JmmNode newNode = new JmmNodeImpl(elseBlock.getKind());
            newNode.put("col", jmmNode.get("col"));
            newNode.put("line", jmmNode.get("line"));
            for (JmmNode child : elseBlock.getChildren())
                newNode.add(child);

            jmmNode.replace(newNode);
        }
        this.counter++;
        return true;
    }

    private Boolean visitWhileStatement(JmmNode jmmNode, String s) {
        var condition = getBoolean(jmmNode.getJmmChild(0).getJmmChild(0));
        if (condition == null || condition) return false;

        jmmNode.getJmmParent().removeJmmChild(jmmNode.getIndexOfSelf());
        this.counter++;

        return true;
    }

    public int getCounter() {
        return counter;
    }
}
