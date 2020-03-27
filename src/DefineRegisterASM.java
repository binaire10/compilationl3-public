import fg.FgSolution;
import nasm.Nasm;
import nasm.NasmOperand;
import nasm.NasmRegister;
import util.intset.IntSet;

import java.util.*;

public class DefineRegisterASM{
    private Map<Integer, NasmRegister> convertTable;

    public DefineRegisterASM() {
    }

    public void convert(Nasm nasm, FgSolution solution) {
        Map<Integer, Integer> convertTable = new HashMap<>();
        Map<Integer, IntSet> excludeRegister = new HashMap<>();
        Deque<Integer> freeRegister = new ArrayDeque<>(4);
        freeRegister.addAll(List.of(Nasm.REG_EAX, Nasm.REG_EBX, Nasm.REG_ECX, Nasm.REG_EDX));
        for (int i = 0; i < nasm.getTempCounter(); i++) {
            excludeRegister.put(i, new IntSet(4));
        }
        for (var inst : nasm.listeInst) {
            IntSet in = solution.in.get(inst);
            IntSet out = solution.out.get(inst);

            if(inst.destination instanceof NasmRegister && ((NasmRegister) inst.destination).color != Nasm.REG_UNK && ((NasmRegister) inst.destination).color >= 0)
                for (int i = 0; i < in.getSize(); i++) {
                    if(out.isMember(i))
                        excludeRegister.get(i).add(((NasmRegister) inst.destination).color);
                }

            if(inst.source instanceof NasmRegister && ((NasmRegister) inst.source).color != Nasm.REG_UNK && ((NasmRegister) inst.source).color >= 0)
                for (int i = 0; i < in.getSize(); i++) {
                    if(in.isMember(i))
                        excludeRegister.get(i).add(((NasmRegister) inst.source).color);
                }

        }

        for (var inst : nasm.listeInst) {
            IntSet def = solution.def.get(inst);

            for (int i = 0; i < def.getSize(); i++)
                if(def.isMember(i)) {
                    Queue<Integer> pending = new ArrayDeque<>(4);
                    do {
                        int c = freeRegister.poll();
                        if (excludeRegister.get(i).isMember(c))
                            pending.add(c);
                        else {
                            convertTable.put(i, c);
                            while (!pending.isEmpty()) {
                                freeRegister.push(pending.poll());
                            }
                        }
                    } while (!pending.isEmpty());
                }

            IntSet in = solution.in.get(inst);
            IntSet out = solution.out.get(inst);


            if(inst.source instanceof  NasmRegister && ((NasmRegister) inst.source).color == Nasm.REG_UNK) {
                int val = ((NasmRegister) inst.source).val;
                ((NasmRegister) inst.source).colorRegister(convertTable.get(val));
            }
            if(inst.destination instanceof  NasmRegister && ((NasmRegister) inst.destination).color == Nasm.REG_UNK) {
                int val = ((NasmRegister) inst.destination).val;
                ((NasmRegister) inst.destination).colorRegister(convertTable.get(val));
            }

            for (int i = 0; i < def.getSize(); i++)
                if(in.isMember(i) && !out.isMember(i)) {
                    freeRegister.push(convertTable.remove(i));
                }
        }
    }

    public NasmOperand visit(NasmRegister operand) {
        return convertTable.get(operand.val);
    }
}
