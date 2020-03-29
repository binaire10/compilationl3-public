package ig;

import fg.FgSolution;
import nasm.Nasm;
import nasm.NasmOperand;
import nasm.NasmRegister;
import util.graph.ColorGraph;
import util.graph.Graph;
import util.graph.Node;
import util.graph.NodeList;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

public class Ig {
    public Graph graph;
    public FgSolution fgs;
    public int regNb;
    public Nasm nasm;
    public Node[] int2Node;
    private ColorGraph colorGraph;


    public Ig(FgSolution fgs) {
        this.fgs = fgs;
        this.graph = new Graph();
        this.nasm = fgs.nasm;
        this.regNb = this.nasm.getTempCounter();
        this.int2Node = new Node[regNb];
        for (int i = 0; i < regNb; i++)
            int2Node[i] = graph.newNode();
        build();
        construction();
        allocateRegisters();
    }

    private void build() {
        for (var value : fgs.in.values())
            for (int i = 0; i < value.getSize(); i++)
                if (value.isMember(i))
                    for (int j = i + 1; j < value.getSize(); j++)
                        if (value.isMember(j))
                            graph.addNOEdge(int2Node[i], int2Node[j]);
        for (var value : fgs.out.values())
            for (int i = 0; i < value.getSize(); i++)
                if (value.isMember(i))
                    for (int j = i + 1; j < value.getSize(); j++)
                        if (value.isMember(j))
                            graph.addNOEdge(int2Node[i], int2Node[j]);
        colorGraph = new ColorGraph(graph, 4, getPrecoloredTemporaries());
    }

    public void construction() {
        colorGraph.coloration();
    }

    public int[] getPrecoloredTemporaries() {
        return Stream.concat(
                nasm.listeInst.stream().map(c-> c.destination),
                nasm.listeInst.stream().map(c-> c.source)
        )
                .filter(Objects::nonNull)
                .filter(NasmOperand::isGeneralRegister)
                .map(NasmRegister.class::cast)
                .sorted(Comparator.comparingInt(c -> c.val))
                .distinct()
                .mapToInt(c -> c.color)
                .toArray();
    }


    public void allocateRegisters() {
        NasmRegister[] registers = Stream.concat(
                nasm.listeInst.stream().map(c-> c.destination),
                nasm.listeInst.stream().map(c-> c.source)
        )
                .filter(Objects::nonNull)
                .filter(NasmOperand::isGeneralRegister)
                .distinct()
                .map(NasmRegister.class::cast)
                .sorted(Comparator.comparingInt(c -> c.val)).toArray(NasmRegister[]::new);
        for (int i = 0; i < registers.length; i++)
            registers[i].color = colorGraph.couleur[registers[i].val];
    }


    public void affiche(String baseFileName) {
        String fileName;
        PrintStream out = System.out;

        if (baseFileName != null) {
            try {
                baseFileName = baseFileName;
                fileName = baseFileName + ".ig";
                out = new PrintStream(fileName);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        for (int i = 0; i < regNb; i++) {
            Node n = this.int2Node[i];
            out.print(n + " : ( ");
            for (NodeList q = n.succ(); q != null; q = q.tail) {
                out.print(q.head.toString());
                out.print(" ");
            }
            out.println(")");
        }
    }
}
	    
    

    
    
