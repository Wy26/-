package com.company;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 词法分析
 */
public class LexicalAnalysis {
    //语言类型
    static final String LINGGUSTIC_TYPE = "C LANGUAGE";
    //存储已知的标识符
    private List<String> identifiers;
    //存储CHAR类型变量名
    private List<String> idInChars;
    //单词的类别码列表
    private final List<String> catagoryCode;
    //单词名称列表
    private final List<String> words;
    {
        idInChars = new ArrayList<>();
        identifiers = new ArrayList<>();
        words = new ArrayList<>();
        catagoryCode = new ArrayList<>();
    }

    /*标志旗!
      flag = 1 : 缩进
      flag = 2 : 前一个符号为','
      flag = 3 : 前一个字符为空格
      flag = 4 : 前一个字符为')'
      flag = 5 : 拆开空括号
      flag = 6 : 前一个字符为+

      flag = 8 : void声明状态
      flag = 9 : int声明变量状态
      flag = 10 : char声明变量状态
      flag = 11 : 变量赋值状态
      flag = 12 : 对比状态
     */
    private int flag;
    //源代码字符指针
    private int pointer;
    //单词缓存
    private String value;
    //源代码
    private String sourceCode;

    {
        sourceCode = "";
        value = "";
    }
    //源代码文件引用
    private final File sourceFile;

    public LexicalAnalysis(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public LexicalAnalysis(String absolutePath) {
        this(new File(absolutePath));
    }

    /**
     * 对读入内存的源程序进行处理
     */
    public void process() {
        //读入
        load();
        //分词
        splitWords();
        //匹配类别码
        formulate();
        //输出
    }

    /**
     * 读取源代码
     */
    private void load() {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
            int v;
            //字节读取缓存
            byte[] cache = new byte[16];

            while ((v = bis.read(cache)) != -1) {
                for (int i = 0; i < v; i++)
                    //因为源代码中并不会出现中文,所以可以使用这种方式进行转换
                    sourceCode += (char) cache[i];
            }

            System.out.println(sourceCode);
        } catch (IOException ex) {
            System.err.println("读取源代码时出现未知错误");
        }
    }

    /**
     * 将源代码进行分词
     */
    private void splitWords() {
        //当前字符缓存
        char ch;
        //处于自定义字符串中
        boolean isString = false;

        while (pointer < sourceCode.length()) {
            ch = next();
            //单引号和双引号不加入单词列表
            if (ch == 39 ||
                    ch == 34) {
                isString = !isString;
                continue;
            }

            if (!isString) {
                switch (ch) {
                    case ' ':
                        processSpacing();
                        continue;
                    case ',':
                        processComma();
                        continue;
                    case '(':
                        processLeftBracket();
                        continue;
                    case ')':
                        processRightBraket();
                        continue;
                    case ';':
                        processSemicolon();
                        continue;
                    case '\r':
                        next();
                        continue;
                    case '{':
                        processLeftBrace();
                        continue;
                    case '}':
                        addChar('}');
                        continue;
                    case '-':
                    case '[':
                    case ']':
                    case '/':
                    case '*':
                        addWordAndChar(ch);
                        continue;
                    case '+':
                        addWordAndChar('+');
                        hoistTheFlag(6);
                        continue;
                    default:
                        char nextChar;
                        if (ch == '=' ||
                                ch == '!' ||
                                ch == '<' ||
                                ch == '>') {
                            addWord();
                            if ((nextChar = sourceCode.charAt(pointer)) == '=') {
                                addWord(ch + "" + nextChar);
                                next();
                            } else {
                                addChar(ch);
                            }
                            continue;
                        }
                }
            }
            value += ch;
            downTheFlag();
        }
    }

    /**
     * 将分词程序分出的单词与类别码进行匹配
     */
    private void formulate() {
        //存储已知的标识符
        //当前状态的前一个状态
        int pre = 0;
        //记录当前判断过的字符个数
        int num = 0;

        for (String word : words) {
            //个数+1
            num++;
            value = word;

            switch (value) {
                case "const":
                    catagoryCode.add("CONSTTK");
                    continue;
                case "int":
                    hoistTheFlag(9);
                    catagoryCode.add("INTTK");
                    continue;
                case "char":
                    hoistTheFlag(10);
                    catagoryCode.add("CHARTK");
                    continue;
                case "void":
                    hoistTheFlag(8);
                    catagoryCode.add("VOIDTK");
                    continue;
                case "main":
                    catagoryCode.add("MAINTK");
                    continue;
                case "if":
                    catagoryCode.add("IFTK");
                    continue;
                case "else":
                    catagoryCode.add("ELSETK");
                    continue;
                case "do":
                    catagoryCode.add("DOTK");
                    continue;
                case "while":
                    catagoryCode.add("WHILETK");
                    continue;
                case "for":
                    catagoryCode.add("FORTK");
                    continue;
                case "scanf":
                    catagoryCode.add("SCANFTK");
                    continue;
                case "printf":
                    catagoryCode.add("PRINTFTK");
                    continue;
                case "return":
                    catagoryCode.add("RETURNTK");
                    continue;
                case "+":
                    if (judgeTheFlag(12) ||
                            (pre == 10 && judgeTheFlag(11)) ||
                            idInChars.contains(words.get(num - 3))) {
                        catagoryCode.add("CHARCON");
                        if (judgeTheFlag(12)) {
                            downTheFlag();
                        }
                    } else {
                        catagoryCode.add("PLUS");
                    }
                    continue;
                case "-":
                    if (pre == 10 &&
                            judgeTheFlag(11)) {
                        catagoryCode.add("CHARCON");
                    } else {
                        catagoryCode.add("MINU");
                    }
                    continue;
                case "*":
                    if (judgeTheFlag(12) ||
                            (pre == 10 &&
                                    judgeTheFlag(11))) {
                        catagoryCode.add("CHARCON");
                        if (judgeTheFlag(12)) downTheFlag();
                    } else {
                        catagoryCode.add("MULT");
                    }
                    continue;
                case "/":
                    if (judgeTheFlag(12) ||
                            (pre == 10 && judgeTheFlag(11)) ||
                            words.get(num - 3).equals("printf")) {
                        catagoryCode.add("CHARCON");
                        if (judgeTheFlag(12)) {
                            downTheFlag();
                        }
                    } else {
                        catagoryCode.add("DIV");
                    }
                    continue;
                case "<":
                    catagoryCode.add("LSS");
                    continue;
                case "<=":
                    catagoryCode.add("LEQ");
                    continue;
                case ">":
                    catagoryCode.add("GRE");
                    continue;
                case ">=":
                    catagoryCode.add("GEQ");
                    continue;
                case "==":
                    hoistTheFlag(12);
                    catagoryCode.add("EQL");
                    continue;
                case "!=":
                    catagoryCode.add("NEQ");
                    continue;
                case "=":
                    if (judgeTheFlag(9) ||
                            judgeTheFlag(10)) {
                        pre = flag;
                    }
                    hoistTheFlag(11);
                    catagoryCode.add("ASSIGN");
                    continue;
                case ";":
                    downTheFlag();
                    pre = 0;
                    catagoryCode.add("SEMICN");
                    continue;
                case ",":
                    if (judgeTheFlag(11)) {
                        hoistTheFlag(pre);
                        pre = 0;
                    }
                    catagoryCode.add("COMMA");
                    continue;
                case "(":
                    catagoryCode.add("LPARENT");
                    continue;
                case ")":
                    downTheFlag();
                    pre = 0;
                    catagoryCode.add("RPARENT");
                    continue;
                case "[":
                    if (judgeTheFlag(9) ||
                            judgeTheFlag(10)) {
                        pre = flag;
                        hoistTheFlag(0);
                    }
                    catagoryCode.add("LBRACK");
                    continue;
                case "]":
                    if (judgeTheFlag(0)) {
                        hoistTheFlag(pre);
                    }
                    catagoryCode.add("RBRACK");
                    continue;
                case "{":
                    catagoryCode.add("LBRACE");
                    continue;
                case "}":
                    catagoryCode.add("RBRACE");
                    continue;
                default:
                    if (identifiers.contains(value)) {
                        catagoryCode.add("IDENFR");
                    } else if (judgeTheFlag(11) && pre == 10 ) {
                        catagoryCode.add("CHARCON");
                    } else if (judgeTheFlag(8) ||
                            judgeTheFlag(10) ||
                            judgeTheFlag(9)) {
                        catagoryCode.add("IDENFR");
                        identifiers.add(value);
                        //记录char类型的变量名
                        if (judgeTheFlag(10)) idInChars.add(value);
                        //void
                        if (judgeTheFlag(8)) {
                            downTheFlag();
                        }
                    } else if (isNum(value)) {
                        catagoryCode.add("INTCON");
                    } else if (value.length() == 1) {
                        catagoryCode.add("CHARCON");
                    } else {
                        catagoryCode.add("STRCON");
                    }
            }
        }
    }

    private boolean isNum(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 用于返回代码文件中的下一个字符
     *
     * @return 当前指针所指向的字符
     */
    private char next() {
        return sourceCode.charAt(pointer++);
    }

    /**
     * 指针前移一位
     */
    private void previous() {
        pointer -= 1;
    }
    private void hoistTheFlag(int state) {
        flag = state;
    }
    private void downTheFlag() {
        flag = 0;
    }

    private boolean judgeTheFlag(int nowState) {
        return flag == nowState;
    }

    /**
     * 清空单词缓存以进行下一次存储
     */
    private void clearValue() {
        value = "";
    }

    /**
     * 将单词缓存中存储的单词加入到单词名称列表
     */
    private void addWord() {
        if (value.length() != 0) addWord(value);
    }

    private void addWord(String str) {
        words.add(str);
        clearValue();
    }

    /**
     * 将一个保留字符加入到单词名称列表
     *
     * @param ch 添加的保留字符
     */
    private void addChar(Character ch) {
        words.add(ch.toString());
    }

    /**
     * 将单词缓存中存储的单词和一个保留字符加入到单词名称列表
     *
     * @param ch 添加的保留字符
     */
    private void addWordAndChar(Character ch) {
        this.addWord();
        this.addChar(ch);
    }

    /**
     * 处理空格
     */
    private void processSpacing() {
        //缩进处理
        while (pointer < sourceCode.length()) {
            if (next() == ' ') {
                hoistTheFlag(1);
                continue;
            }
            previous();
            break;
        }

        if (judgeTheFlag(1) ||
                judgeTheFlag(2) ||
                judgeTheFlag(6)) {
            downTheFlag();
        } else {
            hoistTheFlag(3);
            addWord();
        }
    }

    /**
     * 处理逗号
     */
    private void processComma() {
        addWordAndChar(',');
        hoistTheFlag(2);
//        clearValue();
    }

    /**
     * 处理左括号
     */
    private void processLeftBracket() {
        if (judgeTheFlag(3)) {
            downTheFlag();
            addChar('(');
        } else {
            addWordAndChar('(');
            hoistTheFlag(2);
//            clearValue();
        }
    }
    private void processRightBraket() {
        if (judgeTheFlag(2)) {
            downTheFlag();
            addChar(')');
        } else {
            addWordAndChar(')');
        }
        //解决多重右括号问题
        hoistTheFlag(2);
    }
    private void processSemicolon() {
        if (judgeTheFlag(2)) {
            downTheFlag();
            addChar(';');
        } else {
            addWordAndChar(';');
        }
    }
    private void processLeftBrace() {
        addWordAndChar('{');
    }

    public List<String> getCatagoryCode() {
        return catagoryCode;
    }

    public List<String> getWords() {
        return words;
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public File getSourceFile() {
        return sourceFile;
    }
}