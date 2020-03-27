import c3a.*;
import sa.*;
import ts.Ts;

public class Sa2c3a extends SaDepthFirstVisitor<C3aOperand> {
    private C3a c3a;

    public Sa2c3a(SaNode saRoot, Ts table) {
        c3a = new C3a();
        saRoot.accept(this);
    }

    @Override
    public C3aOperand visit(SaDecFonc node) {
        c3a.ajouteInst(new C3aInstFBegin(node.tsItem, "entree fonction"));
        node.getCorps().accept(this);
        c3a.ajouteInst(new C3aInstFEnd(""));
        return null;
    }

    @Override
    public C3aOperand visit(SaExp node) {
        return node.accept(this);
    }

    @Override
    public C3aOperand visit(SaExpInt node) {
        return new C3aConstant(node.getVal());
    }

    @Override
    public C3aOperand visit(SaExpVar node) {
        return node.getVar().accept(this);
    }

    @Override
    public C3aOperand visit(SaInstEcriture node) {
        c3a.ajouteInst(new C3aInstWrite(node.getArg().accept(this), ""));
        return null;
    }

    @Override
    public C3aOperand visit(SaInstTantQue node) {
        C3aLabel top = c3a.newAutoLabel();
        C3aLabel bottom = c3a.newAutoLabel();
        c3a.addLabelToNextInst(top);
        C3aOperand test = node.getTest().accept(this);
        c3a.ajouteInst(new C3aInstJumpIfEqual(c3a.False, test, bottom, ""));
        node.getFaire().accept(this);
        c3a.ajouteInst(new C3aInstJump(top, ""));
        c3a.addLabelToNextInst(bottom);
        return null;
    }

    @Override
    public C3aOperand visit(SaInstAffect node) {
        c3a.ajouteInst(new C3aInstAffect(node.getRhs().accept(this), node.getLhs().accept(this), ""));
        return null;
    }

    @Override
    public C3aOperand visit(SaVarSimple node) {
        return new C3aVar(node.tsItem, null);
    }

    @Override
    public C3aOperand visit(SaLExp node) {
        if(node.getQueue() != null)
            node.getQueue().accept(this);
        c3a.ajouteInst(new C3aInstParam(node.getTete().accept(this), ""));
        return null;
    }

    @Override
    public C3aOperand visit(SaAppel node) {
        if(node.getArguments() != null)
        node.getArguments().accept(this);
        c3a.ajouteInst(new C3aInstCall(new C3aFunction(node.tsItem), null, ""));
        return super.visit(node);
    }

    @Override
    public C3aOperand visit(SaExpAppel node) {
        if(node.getVal().getArguments() != null)
            node.getVal().getArguments().accept(this);
        var r = c3a.newTemp();
        c3a.ajouteInst(new C3aInstCall(new C3aFunction(node.getVal().tsItem), r, ""));
        return r;
    }

    @Override
    public C3aOperand visit(SaExpAdd node) {
        C3aTemp tmp = c3a.newTemp();
        C3aOperand op2 = node.getOp2().accept(this);
        C3aOperand op1 = node.getOp1().accept(this);
        c3a.ajouteInst(new C3aInstAdd(op1, op2, tmp, ""));
        return tmp;
    }

    @Override
    public C3aOperand visit(SaExpSub node) {
        C3aTemp tmp = c3a.newTemp();
        C3aOperand op2 = node.getOp2().accept(this);
        C3aOperand op1 = node.getOp1().accept(this);
        c3a.ajouteInst(new C3aInstSub(op1, op2, tmp, ""));
        return tmp;
    }

    @Override
    public C3aOperand visit(SaExpMult node) {
        C3aTemp tmp = c3a.newTemp();
        C3aOperand op2 = node.getOp2().accept(this);
        C3aOperand op1 = node.getOp1().accept(this);
        c3a.ajouteInst(new C3aInstMult(op1, op2, tmp, ""));
        return tmp;
    }

    @Override
    public C3aOperand visit(SaExpDiv node) {
        C3aTemp tmp = c3a.newTemp();
        C3aOperand op2 = node.getOp2().accept(this);
        C3aOperand op1 = node.getOp1().accept(this);
        c3a.ajouteInst(new C3aInstDiv(op1, op2, tmp, ""));
        return tmp;
    }

    @Override
    public C3aOperand visit(SaExpInf node) {
        var r = c3a.newTemp();
        var op2 = node.getOp2().accept(this);
        var op1 = node.getOp1().accept(this);
        var bottom = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstAffect(c3a.True, r, ""));
        c3a.ajouteInst(new C3aInstJumpIfLess(op1, op2, bottom, ""));
        c3a.ajouteInst(new C3aInstAffect(c3a.False, r, ""));
        c3a.addLabelToNextInst(bottom);
        return r;
    }

    @Override
    public C3aOperand visit(SaExpEqual node) {
        var r = c3a.newTemp();
        var op2 = node.getOp2().accept(this);
        var op1 = node.getOp1().accept(this);
        var bottom = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstAffect(c3a.True, r, ""));
        c3a.ajouteInst(new C3aInstJumpIfEqual(op1, op2, bottom, ""));
        c3a.ajouteInst(new C3aInstAffect(c3a.False, r, ""));
        c3a.addLabelToNextInst(bottom);
        return r;
    }

    @Override
    public C3aOperand visit(SaExpAnd node) {
        var r = c3a.newTemp();
        var op1 = node.getOp1().accept(this);
        var op2 = node.getOp2().accept(this);
        var elseB = c3a.newAutoLabel();
        var bottom = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstJumpIfEqual(op1, c3a.False, bottom, ""));
        c3a.ajouteInst(new C3aInstJumpIfEqual(op2, c3a.False, bottom, ""));
        c3a.ajouteInst(new C3aInstAffect(c3a.True, r, ""));
        c3a.ajouteInst(new C3aInstJump(elseB, ""));
        c3a.addLabelToNextInst(bottom);
        c3a.ajouteInst(new C3aInstAffect(c3a.False, r, ""));
        c3a.addLabelToNextInst(elseB);
        return r;
    }

    @Override
    public C3aOperand visit(SaExpOr node) {
        var r = c3a.newTemp();
        var op1 = node.getOp1().accept(this);
        var op2 = node.getOp2().accept(this);
        var bottom = c3a.newAutoLabel();
        var elseB = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstJumpIfNotEqual(op1, c3a.False, elseB, ""));
        c3a.ajouteInst(new C3aInstJumpIfNotEqual(op2, c3a.False, elseB, ""));
        c3a.ajouteInst(new C3aInstAffect(c3a.False, r, ""));
        c3a.ajouteInst(new C3aInstJump(bottom, ""));
        c3a.addLabelToNextInst(elseB);
        c3a.ajouteInst(new C3aInstAffect(c3a.True, r, ""));
        c3a.addLabelToNextInst(bottom);
        return r;
    }

    @Override
    public C3aOperand visit(SaExpNot node) {
        var r = node.getOp1().accept(this);
        var bottom = c3a.newAutoLabel();
        var invTrue = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstJumpIfEqual(r, c3a.True, invTrue, ""));
        c3a.ajouteInst(new C3aInstAffect(c3a.True, r, ""));
        c3a.ajouteInst(new C3aInstJump(bottom, ""));
        c3a.addLabelToNextInst(invTrue);
        c3a.ajouteInst(new C3aInstAffect(c3a.False, r, ""));
        c3a.addLabelToNextInst(bottom);
        return r;
    }

    @Override
    public C3aOperand visit(SaExpLire node) {
        var r = c3a.newTemp();
        c3a.ajouteInst(new C3aInstRead(r, ""));
        return r;
    }

    @Override
    public C3aOperand visit(SaInstSi node) {
        var r = node.getTest().accept(this);
        var bottom = c3a.newAutoLabel();
        c3a.ajouteInst(new C3aInstJumpIfEqual(r, c3a.False, bottom, ""));
        node.getAlors().accept(this);
        if(node.getSinon() != null) {
            var ebottom = c3a.newAutoLabel();
            c3a.ajouteInst(new C3aInstJump(ebottom, ""));
            c3a.addLabelToNextInst(bottom);
            node.getSinon().accept(this);
            c3a.addLabelToNextInst(ebottom);
        }
        else
            c3a.addLabelToNextInst(bottom);
        return null;
    }

    @Override
    public C3aOperand visit(SaInstRetour node) {
        c3a.ajouteInst(new C3aInstReturn(node.getVal().accept(this), ""));
        return null;
    }

    @Override
    public C3aOperand visit(SaVarIndicee node) {
        return new C3aVar(node.tsItem, node.getIndice().accept(this));
    }

    public C3a getC3a() {
        return c3a;
    }
}
