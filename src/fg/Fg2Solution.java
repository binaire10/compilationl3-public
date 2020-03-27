package fg;

import nasm.Nasm;
import nasm.NasmInst;
import nasm.NasmOperand;
import nasm.NasmRegister;
import util.graph.Node;
import util.graph.NodeList;
import util.intset.IntSet;

public class Fg2Solution {
    private Fg fg;
    private FgSolution solution;

    public boolean testDef(NasmOperand operand) {
        return operand instanceof NasmRegister && ((NasmRegister) operand).color == Nasm.REG_UNK;
    }

    public Fg2Solution(Fg fg, FgSolution solution) {
        this.fg = fg;
        this.solution = solution;
        int count = solution.nasm.getTempCounter();
        IntSet globalDef = new IntSet(count);
        for(NasmInst inst : solution.nasm.listeInst) {
            boolean testDest= testDef(inst.destination);
            boolean testSrc  = testDef(inst.source);
            IntSet def = new IntSet(count);
            IntSet in  = new IntSet(count);
            IntSet out = new IntSet(count);
            IntSet use = new IntSet(count);
            if(testDest) {
                int val = ((NasmRegister)inst.destination).val;
                if(!globalDef.isMember(val)){
                    def.add(val);
                    globalDef.add(val);
                }
                else
                    use.add(val);
            }
            if(testSrc){
                int val = ((NasmRegister)inst.source).val;
                if(!globalDef.isMember(val)){
                    def.add(val);
                    globalDef.add(val);
                }
                else
                    use.add(val);
            }
            solution.def.put(inst, def);
            solution.in.put(inst, in);
            solution.out.put(inst, out);
            solution.use.put(inst, use);
        }

        var nodes = fg.graph.nodeArray();
        boolean isUpdate = true;
        while (isUpdate) {
            isUpdate = false;
            for (Node s : nodes) {
                NasmInst inst = fg.node2Inst.get(s);
                IntSet def = solution.def.get(inst);
                IntSet inP = solution.in.get(inst);
                IntSet outP = solution.out.get(inst);
                IntSet use = solution.use.get(inst);

                IntSet in = outP.copy().minus(def).union(use);
                IntSet out = new IntSet(outP.getSize());
                out = new IntSet(out.getSize());

                for (NodeList succ = s.succ(); succ != null; succ = succ.tail)
                    out.union(solution.in.get(fg.node2Inst.get(succ.head)));


                if(!isUpdate && (!in.equal(inP) || !out.equal(outP))) {
                    solution.in.put(inst, in);
                    solution.out.put(inst, out);
                    isUpdate = true;
                }
            }
        }
    }
}
