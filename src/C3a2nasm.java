import c3a.*;
import nasm.*;
import ts.Ts;
import ts.TsItemFct;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class C3a2nasm implements C3aVisitor<NasmOperand> {
    private Nasm nasm;
    private Ts table;
    private boolean hashReturn;
    public static final boolean DEBUG = true;

    public static NasmRegister createColorRegister(int color) {
        NasmRegister register = new NasmRegister(0);
        register.colorRegister(color);
        return register;
    }

    private final NasmRegister registerEAX = createColorRegister(Nasm.REG_EAX);
    private final NasmRegister registerEBX = createColorRegister(Nasm.REG_EBX);
    private final NasmRegister registerECX = createColorRegister(Nasm.REG_ECX);
    private final NasmRegister registerEDX = createColorRegister(Nasm.REG_EDX);
    private final NasmRegister registerESP = createColorRegister(Nasm.REG_ESP);
    private final NasmRegister registerEBP = createColorRegister(Nasm.REG_EBP);
    private final NasmAddress returnAddr = new NasmAddress(registerEBP, '+', new NasmConstant(2));
    private NasmConstant allocateStack;
    private int paramCount;

    private Map<C3aOperand, NasmOperand> operandMap;
    private Map<C3aInstFBegin, NasmOperand> beginMap;

    public C3a2nasm(C3a c3a, Ts table) {
        this.table = table;
        nasm = new Nasm(table);

        operandMap = new ConcurrentHashMap<>();
        beginMap = new HashMap<>();

        NasmOperand main = null;

        for (var inst : c3a.listeInst) {
            if(inst.label != null)
                operandMap.put(inst.label, new NasmLabel(inst.label.toString()));
            if(inst instanceof C3aInstFBegin) {
                String symbol = ((C3aInstFBegin) inst).val.identif;
                NasmLabel label = new NasmLabel(symbol);
                beginMap.put((C3aInstFBegin) inst, label);
                if(symbol.equals("main"))
                    main = label;
            }
        }

        nasm.ajouteInst(new NasmCall(null, main, ""));
        nasm.ajouteInst(new NasmMov(null, registerEBX, new NasmConstant(0), " valeur de retour du programme"));
        nasm.ajouteInst(new NasmMov(null, registerEAX, new NasmConstant(1), ""));
        nasm.ajouteInst(new NasmInt(null, ""));
        paramCount = 0;

        for (var inst : c3a.listeInst) {
            inst.accept(this);
        }
    }

    public Nasm getNasm() {
        return nasm;
    }

    NasmOperand getNasmOperand(C3aOperand operand) {
        if(operand == null)
            return null;
        return operand.accept(this);
    }

    private int functionStackSize(TsItemFct fct) {
        int size = 0;
        Ts fcTable = fct.getTable();
        for(var value : fcTable.variables.values()) {
            if(!value.isParam)
                size += value.taille * 4;
        }
        return size;
    }


    @Override
    public NasmOperand visit(C3aInstFBegin inst) {
        var label = beginMap.get(inst);
        hashReturn = false;
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmPush(label, registerEBP, "sauvegarde la valeur de ebp"));
        nasm.ajouteInst(new NasmMov(null, registerEBP, registerESP, "nouvelle valeur de ebp"));
        allocateStack = new NasmConstant(functionStackSize(inst.val));
        nasm.ajouteInst(new NasmSub(null, registerESP, allocateStack, "allocation des variables locales"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstFEnd inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        if (hashReturn == false){
            nasm.ajouteInst(new NasmAdd(getNasmOperand(inst.label), registerESP, allocateStack, "désallocation des variables locales"));
            nasm.ajouteInst(new NasmPop(null, registerEBP, "restaure la valeur de ebp"));
            nasm.ajouteInst(new NasmRet(null, ""));
        }
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstAdd inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var r = inst.result.accept(this);
        var op2 = inst.op2.accept(this);
        var op1 = inst.op1.accept(this);
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), r, op1, ""));
        nasm.ajouteInst(new NasmAdd(null, r, op2, ""));
        return r;
    }

    @Override
    public NasmOperand visit(C3aInstSub inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var r = inst.result.accept(this);
        var op2 = inst.op2.accept(this);
        var op1 = inst.op1.accept(this);
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), r, op1, ""));
        nasm.ajouteInst(new NasmSub(null, r, op2, ""));
        return r;
    }

    @Override
    public NasmOperand visit(C3aInstDiv inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var r = inst.result.accept(this);
        var op2 = inst.op2.accept(this);
        var op1 = inst.op1.accept(this);
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), registerEAX, op1, ""));
        if(op2 instanceof NasmRegister)
            nasm.ajouteInst(new NasmDiv(null, op2, ""));
        else {
            nasm.ajouteInst(new NasmMov(null, r, op2, ""));
            nasm.ajouteInst(new NasmDiv(null, r, ""));
        }
        nasm.ajouteInst(new NasmMov(null, r, registerEAX, ""));
        return r;
    }

    @Override
    public NasmOperand visit(C3aInstMult inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var r = inst.result.accept(this);
        var op2 = inst.op2.accept(this);
        var op1 = inst.op1.accept(this);
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), r, op1, ""));
        nasm.ajouteInst(new NasmMul(null, r, op2, ""));
        return r;
    }

    @Override
    public NasmOperand visit(C3aInstParam inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmPush(getNasmOperand(inst.label), inst.op1.accept(this), "Param"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstCall inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var count = new NasmConstant(4);
        nasm.ajouteInst(new NasmSub(null, registerESP, count, "allocation mémoire pour la valeur de retour"));
        nasm.ajouteInst(new NasmCall(getNasmOperand(inst.label), getNasmOperand(inst.op1), ""));
        nasm.ajouteInst(new NasmPop(null, getNasmOperand(inst.result), "récupération de la valeur de retour"));
        if(inst.op1.val.nbArgs != 0)
            nasm.ajouteInst(new NasmAdd(null, registerESP, new NasmConstant(inst.op1.val.nbArgs*4), "désallocation des arguments"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstReturn inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        hashReturn = true;
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), returnAddr, inst.op1.accept(this), "ecriture de la valeur de retour"));

        nasm.ajouteInst(new NasmAdd(getNasmOperand(inst.label), registerESP, allocateStack, "désallocation des variables locales"));
        nasm.ajouteInst(new NasmPop(null, registerEBP, "restaure la valeur de ebp"));
        nasm.ajouteInst(new NasmRet(null, ""));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInst inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        // hein ???
        nasm.ajouteInst(new NasmEmpty(getNasmOperand(inst.label), "Nope the fish"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstRead inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmCall(getNasmOperand(inst.label), new NasmLabel("readline"), ""));
        nasm.ajouteInst(new NasmCall(null, new NasmLabel("atoi"), ""));
        nasm.ajouteInst(new NasmSub(null, inst.result.accept(this), registerEAX, ""));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstWrite inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), registerEAX, inst.op1.accept(this), "Write 1"));
        nasm.ajouteInst(new NasmCall(null, new NasmLabel("iprintLF"), "Write 2"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstAffect inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmMov(getNasmOperand(inst.label), inst.result.accept(this), inst.op1.accept(this), "Affect"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstJumpIfLess inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var label = getNasmOperand(inst.label);
        var op1 = inst.op1.accept(this);
        var op2 = inst.op2.accept(this);
        var result = inst.result.accept(this);
        if(op1 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op1, op2, "JumpIfLess 1"));
            nasm.ajouteInst(new NasmJl(null, result, "JumpIfLess 2"));
        }
        else if (op2 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op2, op1, "JumpIfLess 1"));
            nasm.ajouteInst(new NasmJg(null, result, "JumpIfLess 2"));
        }
        else {
            var tmp = nasm.newRegister();
            nasm.ajouteInst(new NasmMov(label, tmp, op1, "JumpIfLess 1"));
            nasm.ajouteInst(new NasmCmp(null, tmp, op2, "on passe par un registre temporaire"));
            nasm.ajouteInst(new NasmJl(null, result, "JumpIfLess 2"));
        }
        return null;
    }

    @Override
    public NasmOperand visit(C3aInstJumpIfEqual inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var label = getNasmOperand(inst.label);
        var op1 = inst.op1.accept(this);
        var op2 = inst.op2.accept(this);
        if(op1 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op1, op2, "JumpIfEqual 1"));
            nasm.ajouteInst(new NasmJe(null, inst.result.accept(this), "JumpIfEqual 2"));
        }
        else if (op2 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op2, op1, "JumpIfEqual 1"));
            nasm.ajouteInst(new NasmJe(null, inst.result.accept(this), ""));
        }
        else {
            var tmp = nasm.newRegister();
            nasm.ajouteInst(new NasmMov(label, tmp, inst.op2.accept(this), "JumpIfEqual 1"));
            nasm.ajouteInst(new NasmCmp(null, tmp, inst.op1.accept(this), "on passe par un registre temporaire"));
            nasm.ajouteInst(new NasmJe(null, inst.result.accept(this), "JumpIfEqual 2"));
        }

        return null;
    }

    @Override
    public NasmOperand visit(C3aInstJumpIfNotEqual inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        var label = getNasmOperand(inst.label);
        var op1 = inst.op1.accept(this);
        var op2 = inst.op2.accept(this);
        if(op1 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op1, op2, "JumpIfEqual 1"));
            nasm.ajouteInst(new NasmJne(null, inst.result.accept(this), "JumpIfNotEqual 2"));
        }
        else if (op2 instanceof NasmRegister) {
            nasm.ajouteInst(new NasmCmp(label, op2, op1, "JumpIfEqual 1"));
            nasm.ajouteInst(new NasmJne(null, inst.result.accept(this), "JumpIfNotEqual 2"));
        }
        else {
            nasm.ajouteInst(new NasmMov(label, registerEAX, inst.op1.accept(this), "JumpIfNotEqual 1"));
            nasm.ajouteInst(new NasmCmp(null, registerEAX, inst.op2.accept(this), ""));
            nasm.ajouteInst(new NasmJne(null, inst.result.accept(this), "JumpIfNotEqual 2"));
        }

        return null;
    }

    @Override
    public NasmOperand visit(C3aInstJump inst) {
        if(DEBUG)
            nasm.ajouteInst(new NasmEmpty(null, inst.toString()));
        nasm.ajouteInst(new NasmJmp(getNasmOperand(inst.label), inst.result.accept(this), "Jump"));
        return null;
    }

    @Override
    public NasmOperand visit(C3aConstant oper) {
        return operandMap.computeIfAbsent(oper, o -> new NasmConstant(oper.val));
    }

    @Override
    public NasmOperand visit(C3aLabel oper) {
        return operandMap.computeIfAbsent(oper, o -> new NasmLabel(oper.toString()));
    }

    @Override
    public NasmOperand visit(C3aTemp oper) {
        return operandMap.computeIfAbsent(oper, o -> nasm.newRegister());
    }

    @Override
    public NasmOperand visit(C3aVar oper) {
        if(oper.index != null)
            return operandMap.computeIfAbsent(oper, o -> new NasmAddress(new NasmLabel(oper.item.identif), '+', oper.index.accept(this)));
        if(oper.item.portee == table)
            return operandMap.computeIfAbsent(oper, o -> new NasmAddress(new NasmLabel(oper.item.identif)));
        if(oper.item.isParam)
            return operandMap.computeIfAbsent(oper, o -> new NasmAddress(registerEBP, '+', new NasmConstant(oper.item.adresse+1)));
        return operandMap.computeIfAbsent(oper, o -> new NasmAddress(registerEBP, '-', new NasmConstant(oper.item.adresse+1)));
    }

    @Override
    public NasmOperand visit(C3aFunction oper) {
        return operandMap.computeIfAbsent(oper, o -> new NasmLabel(oper.val.identif));
    }
}
