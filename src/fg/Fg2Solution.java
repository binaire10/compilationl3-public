package fg;

import nasm.NasmInst;
import nasm.NasmRegister;
import util.graph.Node;
import util.graph.NodeList;
import util.intset.IntSet;

public class Fg2Solution {
    private Fg fg;
    private FgSolution solution;

    public Fg2Solution(Fg fg, FgSolution solution) {
        this.fg = fg;
        this.solution = solution;
        int count = solution.nasm.getTempCounter();
        for(NasmInst inst : solution.nasm.listeInst) {
//            boolean testDest= testDef(inst.destination);
//            boolean testSrc  = testDef(inst.source);
            IntSet def = new IntSet(count);
            IntSet in  = new IntSet(count);
            IntSet out = new IntSet(count);
            IntSet use = new IntSet(count);
            if(inst.destination instanceof NasmRegister) {
                NasmRegister register = (NasmRegister) inst.destination;
                if (register.isGeneralRegister()) {
                    int val = register.val;
                    if (inst.destDef)
                        def.add(val);
                    if (inst.destUse)
                        use.add(val);
                }
            }
            if(inst.source instanceof NasmRegister) {
                NasmRegister register = (NasmRegister) inst.source;
                if (register.isGeneralRegister()) {
                    int val = register.val;
                    if (inst.srcDef)
                        def.add(val);
                    if (inst.srcUse)
                        use.add(val);
                }
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
            ++solution.iterNum;
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


                if ((!in.equal(inP) || !out.equal(outP))) {
                    solution.in.put(inst, in);
                    solution.out.put(inst, out);
                    isUpdate = true;
                }
            }
        }
    }
}
