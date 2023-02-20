package pt.up.fe.comp.optimization.register_allocation;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.Utils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

import java.util.*;

public class Live {
    private final List<Instruction> instructions;
    private FunctionClassMap<Instruction, Boolean> instructionMap;
    private final Map<Instruction, UseDef> useDefMap;
    private final Map<Instruction, InOut> inOutMap;
    private final Map<String, LiveRange> webs;

    public Live(List<Instruction> instructions) {
        this.instructions = instructions;
        this.useDefMap = new HashMap<>();
        this.inOutMap = new HashMap<>();
        this.webs = new HashMap<>();
        this.setInstructionsMap();
        this.buildUseDef();
        this.buildInOut();
        this.buildLiveRanges();
    }

    public void show() {
        System.out.println("Basic Blocks:");
        for (var inst : instructions) {
            String bbx = "BB" + inst.getId() + " ";
            System.out.println( bbx + "-".repeat(Utils.sizeOfDelimiter - bbx.length()));
            inst.show();
            var use = useDefMap.get(inst).getUse();
            var def = useDefMap.get(inst).getDef();
            System.out.println("use: " + String.join(" ", use));
            System.out.println("def: " + String.join(" ", def));
            var in = inOutMap.get(inst).getIn();
            var out = inOutMap.get(inst).getOut();
            System.out.println("in: " + String.join(" ", in));
            System.out.println("out: " + String.join(" ", out));
            System.out.println();
        }

        System.out.println("Liveliness Range:");
        for (var variable : webs.entrySet()) {
            System.out.println(variable.getKey() + ": " + variable.getValue());
        }
    }

    private void buildLiveRanges() {
        for (var instruction : instructions) {
            var inSet = this.inOutMap.get(instruction).getIn();
            var outSet = this.inOutMap.get(instruction).getOut();

            for (String variable : outSet) {
                if (!webs.containsKey(variable)) webs.put(variable, new LiveRange(instruction.getId()));
            }
            for (String variable : inSet) {
                LiveRange range = webs.get(variable);
                if (instruction.getId() > range.getEnd()) range.setEnd(instruction.getId());
            }

            for (String variable : this.useDefMap.get(instruction).getDef()) {
                if (!webs.containsKey(variable)) continue;
                LiveRange range = webs.get(variable);
                if (instruction.getId() > range.getEnd()) range.setEnd(instruction.getId());
            }
        }
    }

    private void buildInOut() {
        for (var inst : instructions) {
            inOutMap.put(inst, new InOut());
        }
        boolean update;
        do {
            update = false;
            for (var inst : instructions) {
                var inOut = this.inOutMap.get(inst);
                var useDef = this.useDefMap.get(inst);

                // IN
                Set<String> use = new HashSet<>(useDef.getUse());
                Set<String> out = new HashSet<>(inOut.getOut());
                Set<String> def = new HashSet<>(useDef.getDef());
                out.removeAll(def);
                use.addAll(out);

                // OUT
                Set<String> newOut = new HashSet<>();
                for (Node node : inst.getSuccessors()) {
                    if (node.getNodeType() != NodeType.INSTRUCTION) continue;
                    newOut.addAll(this.inOutMap.get((Instruction) node).getIn());
                }

                InOut newInOut = new InOut(use, newOut);
                InOut oldInOut = this.inOutMap.replace(inst, newInOut);

                if (!oldInOut.equals(newInOut)) update = true;
            }
        } while (update);
    }

    private void buildUseDef() {
        for (var inst : instructions) {
            this.useDefMap.put(inst, new UseDef());
            this.instructionMap.apply(inst);
        }
    }

    private void setInstructionsMap() {
        this.instructionMap = new FunctionClassMap<>();
        this.instructionMap.put(AssignInstruction.class, this::setDefUse);
        this.instructionMap.put(CallInstruction.class, this::setDefUse);
        this.instructionMap.put(GetFieldInstruction.class, this::setDefUse);
        this.instructionMap.put(PutFieldInstruction.class, this::setDefUse);
        this.instructionMap.put(BinaryOpInstruction.class, this::setDefUse);
        this.instructionMap.put(UnaryOpInstruction.class, this::setDefUse);
        this.instructionMap.put(SingleOpInstruction.class, this::setDefUse);
        this.instructionMap.put(CondBranchInstruction.class, this::setDefUse);
        this.instructionMap.put(GotoInstruction.class, this::setDefUse);
        this.instructionMap.put(ReturnInstruction.class, this::setDefUse);
    }

    private Boolean setDefUse(AssignInstruction instruction)  {
        if (instruction.getDest() instanceof ArrayOperand) {
            this.addUseToMap(instruction, instruction.getDest());
            var op = ((ArrayOperand) instruction.getDest()).getIndexOperands().get(0);
            this.addUseToMap(instruction, op);
        }
        else {
            String def = ((Operand)instruction.getDest()).getName();
            this.useDefMap.get(instruction).addDef(def);
        }
        Instruction rhs = instruction.getRhs();
        switch (rhs.getInstType()) {
            case CALL:
                CallInstruction callInstruction = (CallInstruction) rhs;
                if (callInstruction.getInvocationType().equals(CallType.arraylength)) {
                    this.addUseToMap(instruction, callInstruction.getFirstArg());
                    break;
                }
                if (callInstruction.getInvocationType().equals(CallType.invokespecial)
                        || callInstruction.getInvocationType().equals(CallType.invokevirtual)) {
                    if (!callInstruction.getFirstArg().getType().getTypeOfElement().equals(ElementType.THIS))
                        this.addUseToMap(instruction, callInstruction.getFirstArg());
                }
                for (var element : callInstruction.getListOfOperands())
                    this.addUseToMap(instruction, element);
                break;
            case BINARYOPER:
                this.addUseToMap(instruction, ((BinaryOpInstruction)rhs).getLeftOperand());
                this.addUseToMap(instruction, ((BinaryOpInstruction)rhs).getRightOperand());
                break;
            case UNARYOPER:
                this.addUseToMap(instruction, ((UnaryOpInstruction)rhs).getOperand());
                break;
            case NOPER:
                this.addUseToMap(instruction, ((SingleOpInstruction)rhs).getSingleOperand());
                break;
        }
        return true;
    }
    private Boolean setDefUse(CallInstruction instruction)  {
        if (instruction.getInvocationType().equals(CallType.invokespecial)
                || instruction.getInvocationType().equals(CallType.invokevirtual)) {
            if (!instruction.getFirstArg().getType().getTypeOfElement().equals(ElementType.THIS))
                this.addUseToMap(instruction, instruction.getFirstArg());
        }
        for (var element : instruction.getListOfOperands()) {
            this.addUseToMap(instruction, element);
        }
        return true;
    }
    private Boolean setDefUse(GetFieldInstruction instruction)  {
        return true;
    }
    private Boolean setDefUse(PutFieldInstruction instruction)  {
        this.addUseToMap(instruction, instruction.getThirdOperand());
        return true;
    }
    private Boolean setDefUse(BinaryOpInstruction instruction)  {
        this.addUseToMap(instruction, instruction.getLeftOperand());
        this.addUseToMap(instruction, instruction.getRightOperand());
        return true;
    }
    private Boolean setDefUse(UnaryOpInstruction instruction)  {
        this.addUseToMap(instruction, instruction.getOperand());
        return true;
    }
    private Boolean setDefUse(SingleOpInstruction instruction)  {
        this.addUseToMap(instruction, instruction.getSingleOperand());
        return true;
    }
    private Boolean setDefUse(CondBranchInstruction instruction)  {
        for (var element : instruction.getOperands()) {
            this.addUseToMap(instruction, element);
        }
        return true;
    }
    private Boolean setDefUse(GotoInstruction instruction)  {
        return true;
    }
    private Boolean setDefUse(ReturnInstruction instruction)  {
        this.addUseToMap(instruction, instruction.getOperand());
        return true;
    }

    private void addUseToMap(Instruction instruction, Element element) {
        if (element == null) return;
        if (element.isLiteral()) return;
        Operand operand = (Operand)element;
        if (operand.isParameter()) return;
        String name = operand.getName();
        var useDef = this.useDefMap.get(instruction);
        useDef.addUse(name);
    }

    public Map<String, LiveRange> getWebs() {
        return webs;
    }
}
