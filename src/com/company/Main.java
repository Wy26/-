package com.company;

public class Main {

    public static void main(String[] args) {
        //启动词法分析模块
	    LexicalAnalysis t = new LexicalAnalysis("testfile.txt");
	    t.process();
	    //启动语法分析模块
	    GrammaticalAnalysis grammaticalAnalysis = new GrammaticalAnalysis(t.getWords(), t.getCatagoryCode(), t.getIdentifiers());
	    grammaticalAnalysis.runProgramme();
    }
}
