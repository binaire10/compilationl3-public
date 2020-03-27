import sa.*;
import ts.Ts;
import ts.TsItemFct;
import ts.TsItemVar;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Predicate;

public class Sa2ts extends SaDepthFirstVisitor<Void> {
    private Deque<Ts> global;

    public Sa2ts(SaNode saRoot) {
        global = new LinkedList<>();
        global.add(new Ts());
        saRoot.accept(this);
    }

    TsItemFct existFunction(String name, Predicate<TsItemFct> predicate) {
        Iterator<Ts> iterator = global.descendingIterator();
        while (iterator.hasNext()) {
            Ts local = iterator.next();
            if(local.fonctions.containsKey(name))
                if(predicate.test(local.fonctions.get(name)))
                    return local.getFct(name);
                else
                    break;
        }
        return null;
    }

    TsItemFct existFunction(String name) {
        Iterator<Ts> iterator = global.descendingIterator();
        while (iterator.hasNext()) {
            Ts local = iterator.next();
            if(local.fonctions.containsKey(name))
                return local.fonctions.get(name);
        }
        return null;
    }

    TsItemVar existVariable(String name) {
        Iterator<Ts> iterator = global.descendingIterator();
        while (iterator.hasNext()) {
            Ts local = iterator.next();
            if(local.variables.containsKey(name))
                return local.getVar(name);
        }
        return null;
    }

    TsItemVar existVariable(String name, Predicate<TsItemVar> predicate) {
        Iterator<Ts> iterator = global.descendingIterator();
        while (iterator.hasNext()) {
            Ts local = iterator.next();
            if(local.variables.containsKey(name))
                if(predicate.test(local.variables.get(name)))
                    return local.getVar(name);
                else
                    break;
        }
        return null;
    }

    public Ts getTableGlobale() {
        return global.getFirst();
    }

    @Override
    public Void visit(SaDecTab node) {
        global.getLast().addVar(node.getNom(), node.getTaille());
        return null;
    }

    @Override
    public Void visit(SaDecFonc node) {
        String name = node.getNom();
        global.add(new Ts());
        if(node.getVariable() != null)
            node.getVariable().accept(this);

        for(SaLDec parameters = node.getParametres() ; parameters != null ; parameters = parameters.getQueue())
            global.getLast().addParam(parameters.getTete().getNom());
        node.tsItem = global.getFirst().addFct(name, node.getParametres() == null ? 0 : node.getParametres().length(), global.getLast(), node);
        node.getCorps().accept(this);
        global.removeLast();
        return null;
    }

    @Override
    public Void visit(SaDecVar node) {
        if(existVariable(node.getNom()) != null)
            throw new RuntimeException("Variable name already exist");
        node.tsItem = global.getLast().addVar(node.getNom(), 1);
        return null;
    }

    @Override
    public Void visit(SaVarSimple node) {
        if((node.tsItem = existVariable(node.getNom(), v -> v.taille == 1)) == null)
            throw new RuntimeException("Variable simple not exist `" + node.getNom() + "`");
        assert(global.getLast().variables.containsKey(node.getNom()));
        return null;
    }

    @Override
    public Void visit(SaVarIndicee node) {
        if((node.tsItem = existVariable(node.getNom(), v -> v.taille != 1)) == null)
            throw new RuntimeException("Variable indexed not exist `" + node.getNom() + "`");
        return null;
    }

    @Override
    public Void visit(SaAppel node) {
        int nbArgs = node.getArguments() == null ? 0 : node.getArguments().length();
        if((node.tsItem = existFunction(node.getNom(), f -> f.nbArgs == nbArgs)) == null)
            throw new RuntimeException("Function not exist or bad argument count `" + node.getNom() + "`");
        return null;
    }
}
