import sa.*;
import sc.analysis.DepthFirstAdapter;
import sc.node.*;

import java.io.PrintStream;

public class Sc2sa extends DepthFirstAdapter {
    private SaNode returnValue;
    private int indentation;
    private String baseFileName;
    private String fileName;
    private PrintStream out;

    private <T extends SaNode> T retrieveNode(Node node) {
        if(node == null)
            return null;
        node.apply(this);
        return (T)returnValue;
    }

    private <T extends SaExp> T retrieveExp(Node node) {
        SaLExp saLExp = retrieveNode(node);
//        while (saLExp.getQueue() != null)
//            saLExp = saLExp.getQueue();
        return (T)saLExp.getTete();
    }

    @Override
    public void caseAProgramme(AProgramme node) {
        returnValue = new SaProg(
                retrieveNode(node.getOptdecvar()),
                retrieveNode(node.getListedecfonc())
        );
    }

    @Override
    public void caseAIntegerType(AIntegerType node) {
        returnValue = null;
    }

    @Override
    public void caseAVarSimpleDecVar(AVarSimpleDecVar node) {
        returnValue = new SaDecVar(node.getIdent().getText());
    }

    @Override
    public void caseAArrayVarDecVar(AArrayVarDecVar node) {
        returnValue = new SaDecTab(node.getIdent().getText(), Integer.parseInt(node.getNumber().getText()));
    }

    @Override
    public void caseADecVarDecArg(ADecVarDecArg node) {
        returnValue = new SaLDec(retrieveNode(node.getDecVar()), null);
    }

    @Override
    public void caseADecVarsDecArg(ADecVarsDecArg node) {
        returnValue = new SaLDec(retrieveNode(node.getDecVar()), retrieveNode(node.getDecArg()));
    }

    @Override
    public void caseAAffectationInstrSimple(AAffectationInstrSimple node) {
        returnValue = new SaInstAffect(retrieveNode(node.getVar()), retrieveExp(node.getExp()));
    }

    @Override
    public void caseAWriteInstrSimple(AWriteInstrSimple node) {
        returnValue = new SaInstEcriture(retrieveExp(node.getExp()));
    }

    @Override
    public void caseAReadTerm(AReadTerm node) {
        returnValue = new SaExpLire();
    }

    @Override
    public void caseAProcedureInstrSimple(AProcedureInstrSimple node) {
        node.getCall().apply(this);
    }

    @Override
    public void caseAReturnInstrSimple(AReturnInstrSimple node) {
        returnValue = new SaInstRetour(retrieveExp(node.getExp()));
    }

    @Override
    public void caseAExp(AExp node) {
        node.getExp9().apply(this);
    }

    @Override
    public void caseACommaExp9(ACommaExp9 node) {
        returnValue = new SaLExp(retrieveNode(node.getExp8()), retrieveNode(node.getExp9()));
    }

    @Override
    public void caseAExp8Exp9(AExp8Exp9 node) {
        node.getExp8().apply(this);
        if(returnValue instanceof SaLExp)
            return;
        returnValue = new SaLExp((SaExp)returnValue, null);
    }

    @Override
    public void caseAOrExp8(AOrExp8 node) {
        returnValue = new SaExpOr(retrieveNode(node.getExp8()), retrieveNode(node.getExp7()));
    }

    @Override
    public void caseAExp7Exp8(AExp7Exp8 node) {
        node.getExp7().apply(this);
    }

    @Override
    public void caseAAndExp7(AAndExp7 node) {
        returnValue = new SaExpAnd(retrieveNode(node.getExp7()), retrieveNode(node.getExp6()));
    }

    @Override
    public void caseAExp6Exp7(AExp6Exp7 node) {
        node.getExp6().apply(this);
    }

    @Override
    public void caseAEqualExp6(AEqualExp6 node) {
        returnValue = new SaExpEqual(retrieveNode(node.getExp6()), retrieveNode(node.getExp5()));
    }

    @Override
    public void caseAExp5Exp6(AExp5Exp6 node) {
        node.getExp5().apply(this);
    }

    @Override
    public void caseAInferiorExp5(AInferiorExp5 node) {
        returnValue = new SaExpInf(retrieveNode(node.getExp5()), retrieveNode(node.getExp4()));
    }

    @Override
    public void caseAExp4Exp5(AExp4Exp5 node) {
        node.getExp4().apply(this);
    }

    @Override
    public void caseAPlusExp4(APlusExp4 node) {
        returnValue = new SaExpAdd(retrieveNode(node.getExp4()), retrieveNode(node.getExp3()));
    }

    @Override
    public void caseAMinusExp4(AMinusExp4 node) {
        returnValue = new SaExpSub(retrieveNode(node.getExp4()), retrieveNode(node.getExp3()));
    }

    @Override
    public void caseAExp3Exp4(AExp3Exp4 node) {
        node.getExp3().apply(this);
    }

    @Override
    public void caseAMultExp3(AMultExp3 node) {
        returnValue = new SaExpMult(retrieveNode(node.getExp3()), retrieveNode(node.getExp2()));
    }

    @Override
    public void caseADivExp3(ADivExp3 node) {
        returnValue = new SaExpDiv(retrieveNode(node.getExp3()), retrieveNode(node.getExp2()));
    }

    @Override
    public void caseAModExp3(AModExp3 node) {
        throw new RuntimeException("pas de modulo supporté");
    }

    @Override
    public void caseAExp2Exp3(AExp2Exp3 node) {
        node.getExp2().apply(this);
    }

    @Override
    public void caseAMinusExp2(AMinusExp2 node) {
        returnValue = new SaExpSub(new SaExpInt(0), retrieveNode(node.getExp2()));
    }

    @Override
    public void caseANotExp2(ANotExp2 node) {
        returnValue = new SaExpNot(retrieveNode(node.getExp2()));
    }

    @Override
    public void caseAExp1Exp2(AExp1Exp2 node) {
        node.getExp1().apply(this);
    }

    @Override
    public void caseAArrayAccessExp1(AArrayAccessExp1 node) {
        SaExpVar var = retrieveNode(node.getTerm());
        SaVarSimple varSimple = (SaVarSimple)var.getVar();
        returnValue = new SaExpVar(new SaVarIndicee(varSimple.getNom(), retrieveExp(node.getExp())));
    }

    @Override
    public void caseATermExp1(ATermExp1 node) {
        node.getTerm().apply(this);
    }

    @Override
    public void caseANumberTerm(ANumberTerm node) {
        returnValue = new SaExpInt(Integer.parseInt(node.getNumber().getText()));
    }

    @Override
    public void caseAExprTerm(AExprTerm node) {
        node.getExp().apply(this);
    }

    @Override
    public void caseACallTerm(ACallTerm node) {
        returnValue =  new SaExpAppel(retrieveNode(node.getCall()));
    }

    @Override
    public void caseAIdentTerm(AIdentTerm node) {
        returnValue = new SaExpVar(new SaVarSimple(node.getIdent().getText()));
    }

    @Override
    public void caseACall(ACall node) {
        returnValue = new SaAppel(node.getIdent().getText(), retrieveNode(node.getExp()));
    }

    @Override
    public void caseAVarVar(AVarVar node) {
        returnValue = new SaVarSimple(node.getIdent().getText());
    }

    @Override
    public void caseAArrayVar(AArrayVar node) {

        returnValue = new SaVarIndicee(node.getIdent().getText(), retrieveExp(node.getExp()));
    }

    @Override
    public void caseAIfCondStruct(AIfCondStruct node) {
        returnValue = new SaInstSi(retrieveExp(node.getExp()), retrieveNode(node.getBlockInstr()), retrieveNode(node.getElseStructure()));
    }

    @Override
    public void caseAWhileCondStruct(AWhileCondStruct node) {
        returnValue = new SaInstTantQue(retrieveExp(node.getExp()), retrieveNode(node.getBlockInstr()));
    }

    @Override
    public void caseAElseElseStructure(AElseElseStructure node) {
        node.getBlockInstr().apply(this);
    }

    @Override
    public void caseASimpleInstr(ASimpleInstr node) {
        node.getInstrSimple().apply(this);
    }

    @Override
    public void caseAStructureInstr(AStructureInstr node) {
        node.getCondStruct().apply(this);
    }

    @Override
    public void caseAInstrInstrLists(AInstrInstrLists node) {
        returnValue = new SaLInst(retrieveNode(node.getInstr()), null);
    }

    @Override
    public void caseAInstrBlockInstrLists(AInstrBlockInstrLists node) {
        returnValue = new SaLInst(retrieveNode(node.getInstr()), retrieveNode(node.getInstrLists()));
    }

    @Override
    public void caseABlockBlockInstr(ABlockBlockInstr node) {
        returnValue = new SaInstBloc(retrieveNode(node.getInstrLists()));
    }

    @Override
    public void caseADecfonc(ADecfonc node) {
        returnValue = new SaDecFonc(node.getIdent().getText(), retrieveNode(node.getDecArg()), retrieveNode(node.getOptdecvar()), retrieveNode(node.getBlockInstr()));
    }

    @Override
    public void caseAListedecfonc(AListedecfonc node) {
        returnValue = new SaLDec(retrieveNode(node.getDecfonc()), null);
    }

    @Override
    public void caseAListedecfoncsListedecfonc(AListedecfoncsListedecfonc node) {
        returnValue = new SaLDec(retrieveNode(node.getDecfonc()), retrieveNode(node.getListedecfonc()));
    }

    @Override
    public void caseAOptdecvar(AOptdecvar node) {
        node.getDecArg().apply(this);
    }


    public SaNode getRoot() {
        return returnValue;
    }
}
