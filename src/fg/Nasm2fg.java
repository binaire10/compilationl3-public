package fg;

import nasm.Nasm;
import nasm.NasmInst;
import nasm.NasmJmp;
import nasm.NasmRet;
import util.graph.Node;

public class Nasm2fg {

    public Nasm2fg(Nasm nasm, Fg fg) {
        for (var inst : nasm.listeInst) {
            Node node = fg.graph.newNode();
            if (inst.label != null) {
                fg.label2Inst.put(inst.label.toString(), inst);
                fg.inst2Node.put(inst, node);
                fg.node2Inst.put(node, inst);
            }

            fg.inst2Node.put(inst, node);
            fg.node2Inst.put(node, inst);

        }
        Node previous = null;
        for (int j = 0; j < nasm.listeInst.size(); j++) {
            NasmInst inst = nasm.listeInst.get(j);
            var current = fg.inst2Node.get(inst);

            if(inst.address != null) {
                var l = fg.label2Inst.get(inst.address.toString());
                if(l != null)
                    fg.graph.addEdge(current, fg.inst2Node.get(l));
            }
            if(previous != null) {
                fg.graph.addEdge(previous, current);
            }
            if(inst instanceof NasmRet || inst instanceof NasmJmp)
                previous = null;
            else
                previous = current;
        }
        fg.graph.show(System.out);
    }

}
