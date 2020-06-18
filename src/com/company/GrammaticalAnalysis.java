package com.company;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析
 */
public class GrammaticalAnalysis {
    private final List<String> wordsFromLA;
    private final List<String> categoryCodeFromLA;
    private final List<String> identifiersFromLA;
    private final List<String> result;
    private final List<String> voidTypeheaders;

    private boolean inTheEnd;
    private boolean needRetreat;

    private int retreatStep;
    private String token;
    private int pointer;

    {
        result = new ArrayList<>();
        voidTypeheaders = new ArrayList<>();

        token = "";
        //由于在获取下一个TOKEN字的方法中需要先将单词指针自增1
        // 因此赋值为-1
        pointer = -1;
    }

    public GrammaticalAnalysis(List<String> words, List<String> categoryCode, List<String> identifiers) {
        wordsFromLA = words;
        categoryCodeFromLA = categoryCode;
        identifiersFromLA = identifiers;
    }

    private void setRetreatStep(int step) {
        retreatStep = step;
    }

    /**
     * 用户接口
     */
    public void runProgramme() {
        //取下一个输入符
        nextToken();
        //如果输入符属于<常量声明>SELECT集合
        if (inConstantDeclarationSelection()) {
            //进入到<常量声明>产生式
            gotoConstantDeclaration();
        }
        //如果输入符属于<变量声明>SELECT集合
        if (inVaribleDeclarationSelection()) {
            //进入到<变量声明>产生式
            gotoVariableDeclaration();
        }
        //闭包
        while (true) {
            //输入符属于<有返回值函数定义>SELECT集合
            if (inReturnTypeMethodSelection()) {
                //进入到<有返回值函数定义>产生式
                gotoReturnTypeMethod();
                continue;
            }
            //或者<无返回值函数定义>SELECT集合
            if (inVoidMethodSelection()) {
                //进入到<无返回值函数定义>产生式
                gotoVoidMethod();
                continue;
            }
            break;
        }

        if (inMainSelection()) {
            //必须进入<主函数>产生式
            // 并且不需要再取下一个输入符
            gotoMain();
        } else {
            //发生错误
            // 不是该文法的句子
            throwError();
        }
        //输出规约后的左部
        addToResult("<程序>", true);

        output();
    }

    /**
     * 输出分词结果
     */
    private void output() {
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(new File("output.txt")))) {

            for (int i = 0; i < result.size(); i++) {
                bos.write((result.get(i) + '\n').getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            System.err.println("输出文件时出现未知错误");
        }
    }

    private void addToResult(String traget) {
        result.add(traget);
    }

    private void addToResult(String target, boolean up) {
        if (!inTheEnd) result.add(result.size() - 1, target);
        else
            result.add(target);
    }

    private void gotoMain() {
        nextToken();

        checkToken("main");
        checkToken("(");
        checkToken(")");
        checkToken("{");

        if (inCompoundStatementSelection()) {
            gotoCompoundStatement();
        } else {
            throwError();
        }

        checkToken("}");

        addToResult("<主函数>", true);
    }

    private void gotoVoidMethod() {
        nextToken();

        if (inIdentifierSelection()) {
            //将无返回值的方法名进行记录
            voidTypeheaders.add(token);
            gotoIdentifier();
        } else {
            throwError();
        }

        checkToken("(");

        if (inParameterListSelection()) {
            gotoParameterList();
        } else {
            throwError();
        }

        checkToken(")");
        checkToken("{");

        if (inCompoundStatementSelection()) {
            gotoCompoundStatement();
        } else {
            throwError();
        }

        checkToken("}");

        addToResult("<无返回值函数定义>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoConstantDeclaration() {
        //正闭包
        while (true) {
            //由于产生式由终结符开始,所以需要先取下一个输入符
            nextToken();

            if (inConstantDefineSelection()) {
                gotoConstantDefine();
                //判断下一个输入符是否为分号
                if (!token.equals(";")) throwError();
                //取下一输入符
                nextToken();
                //判断下一个输入符是否仍然需要使用<常量说明>产生式
                if (!inConstantDeclarationSelection()) break;
            } else {
                throwError();
            }
        }
        //输出规约后的左部
        addToResult("<常量说明>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoConstantDefine() {
        do {
            //由于产生式由终结符开始,所以需要先取下一个输入符
            nextToken();
            //如果输入符属于<标识符>SELECT集合
            if (inIdentifierSelection()) {
                //进入<标识符>产生式
                gotoIdentifier();
            } else {
                throwError();
            }
            //判断下一个输入符是否为整数
            if (!token.equals("=")) throwError();
            //取下一个输入符
            nextToken();
            //如果是<整数>的SELECT集合则进入<整数>的产生式
            //防止'+'和加法运算符混淆
            if (!categoryCodeFromLA.get(pointer).equals("CHARCON") && inIntegerSelection()) {
                //进入<整数>产生式
                gotoInteger();
                //如果是<字符>的SELECT集合则进入<字符>的产生式
            } else if (inCharsSelection()) {
                //进入<字符>产生式
                gotoChars();
            } else {
                throwError();
            }
            //判断下一个输入符是否仍然需要使用<常量定义>产生式
        } while (token.equals(","));
        //输出规约后的左部
        addToResult("<常量定义>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoIdentifier() {
        //正闭包
        do {
            //进入<字母>产生式
            if (inAlphabetSelection()) gotoAlphabet();
                //或者进入<数字>产生式
            else if (inNumericalSelection()) gotoNumerical();
            //如果下一个输入符不属于<字符>SELECT集合且不属于<数字>SELECT集合
            // 那么就跳出循环
        } while (inAlphabetSelection() || inNumericalSelection());
    }

    private void gotoAlphabet() {
        nextToken();
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoInteger() {
        //如果有+/-则需要向后取一个输入符
        if (inAddingOperatorSelection()) nextToken();
        //进入<无符号整数>
        gotoUnsignInteger();

        addToResult("<整数>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoUnsignInteger() {
        if (token.equals("0")) {
            //如果是零则直接取下一个输入符
            // 因为任何数字不可能由0开始
            nextToken();
        } else {
            //如果输入符属于<非零数字>SELECT集合
            if (inNoneZeroNumericalSelection()) {
                //进入<非零数字>产生式
                gotoNoneZeroNumerical();
            }
            //闭包
            while (true) {
                //如果输入符属于<数字>SELECT集合
                if (inNumericalSelection()) {
                    //进入<数字>产生式
                    gotoNumerical();
                    //否则跳出循环
                } else {
                    break;
                }
            }
        }
        addToResult("<无符号整数>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoChars() {
        if (inAddingOperatorSelection() || inMultiplyOpertairSelection() || inAlphabetSelection()) {
            nextToken();
        } else {
            gotoNumerical();
        }
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoNumerical() {
        if (token.equals("0")) {
            nextToken();
        } else if (inNoneZeroNumericalSelection()) {
            gotoNoneZeroNumerical();
        }
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoNoneZeroNumerical() {
        nextToken();
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoVariableDeclaration() {
        boolean hasVariables = false;

        if (inVaribleDefineSelection()) {
            do {
                gotoVaribleDefine();
                //出现方法名-变量名混淆之后.需要将指针回退并直接返回,在这种情况下需要break
                //还有可能在一开始就定义函数,导致多输出一行<变量说明>,在这种情况下需要return
                if (needRetreat) {
                    preToken(retreatStep);
                    needRetreat = false;
                    deleteWrongItemFromResult();
                    if (!hasVariables) return;
                    else
                        break;
                }

                if (token.equals(";")) nextToken();
                else
                    throwError();

                hasVariables = true;
            } while (inVaribleDefineSelection());

            addToResult("<变量说明>", true);
        } else throwError();
    }

    private void deleteWrongItemFromResult() {
        deleteWrongItemFromResult(retreatStep);
    }

    private void deleteWrongItemFromResult(int num) {
        for (int i = 0; i < num; i++) result.remove(result.size() - 1);
    }

    private void preToken(int backStep) {
        token = wordsFromLA.get(pointer -= backStep);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoVaribleDefine() {
        //如果属于<类型标识符>SELECT集合
        if (inTypeIdentifierSelection()) {
            //进入<类型标识符>产生式
            gotoTypeIdentifier();
        }

        do {
            if (inIdentifierSelection()) {
                gotoIdentifier();
            } else {
                throwError();
            }
            if (token.equals("(")) {
                //需要回退,因为出现了混淆
                needRetreat = true;
                setRetreatStep(2);
                return;
            }
            //如果<标识符>后有输入符"["那么说明该变量是一个数组元素
            if (token.equals("[")) {
                //取下一个输入符
                nextToken();
                //如果输入<无符号整数>SELECT集合
                if (inUnsignIntegerSelection()) {
                    //进入<无符号整数>产生式
                    gotoUnsignInteger();
                } else {
                    throwError();
                }

                checkToken("]");
            }
        } while (is(","));

        addToResult("<变量定义>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoTypeIdentifier() {
        nextToken();
    }

    private void gotoReturnTypeMethod() {
        if (inAssignHeaderSelection()) {
            gotoAssignHeader();
        } else {
            throwError();
        }

        if (token.equals("(")) {
            nextToken();
        } else {
            throwError();
        }

        if (inParameterListSelection()) {
            gotoParameterList();
        } else {
            throwError();
        }

        checkToken(")");
        checkToken("{");

        if (inCompoundStatementSelection()) {
            gotoCompoundStatement();
        } else {
            throwError();
        }

        checkToken("}");

        addToResult("<有返回值函数定义>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoAssignHeader() {
        //由于以终结符开头,所以要先取下一个输入符
        nextToken();
        //如果属于<标识符>SELECT集合
        if (inIdentifierSelection()) {
            //进入<标识符>产生式
            gotoIdentifier();
        } else {
            throwError();
        }

        addToResult("<声明头部>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoParameterList() {
        do {
            if (inTypeIdentifierSelection()) {
                gotoTypeIdentifier();
            } else {
                throwError();
            }

            if (inIdentifierSelection()) {
                gotoIdentifier();
            } else {
                throwError();
            }
        } while (is(","));

        addToResult("<参数表>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoCompoundStatement() {
        //如果属于<常量说明>SELECT集合
        if (inConstantDeclarationSelection()) {
            //进入<常量说明>产生式
            gotoConstantDeclaration();
        }
        //如果属于<变量说明>SELECT集合
        if (inVaribleDeclarationSelection()) {
            //进入<变量说明>产生式
            gotoVariableDeclaration();
        }
        //如果属于<语句列>SELECT集合
        if (inStatementColumnSelection()) {
            //进入<语句列>产生式
            gotoStatementColumn();
        } else {
            throwError();
        }

        addToResult("<复合语句>", true);
    }

    private void gotoStatementColumn() {
        while (inStatementSelection()) {
            gotoStatement();
        }

        addToResult("<语句列>", true);
    }

    private void gotoStatement() {
        boolean alreadyIn = false;

        if (inConditionalStatementSelection()) {
            gotoConditionalStatement();
            alreadyIn = true;
        } else if (inLoopStatementSelection()) {
            gotoLoopStatement();

            alreadyIn = true;
        } else if (token.equals("{")) {
            nextToken();

            gotoStatementColumn();
            checkToken("}");

            alreadyIn = true;
        } else if (inReadStatementSelection()) {
            gotoReadStatement();
            checkToken(";");

            alreadyIn = true;
        } else if (inWriteStatementSelection()) {
            gotoWriteStatement();
            checkToken(";");

            alreadyIn = true;
        } else if (inReturnStatementSelection()) {
            gotoReturnStatement();
            checkToken(";");

            alreadyIn = true;
        }

        if (inHasReturnValueCallStatementSelection() && !alreadyIn) {
            gotoHasReturnValueCallStatement(false);
            //如果出现了混淆则需要继续向下判断
            if (needRetreat) {
                preToken(retreatStep);
                deleteWrongItemFromResult();
                needRetreat = false;
            } else {
                checkToken(";");

                alreadyIn = true;
            }
        }
        if (!alreadyIn && inValuationSelection()) {
            gotoValuation();

            checkToken(";");
        } else if (!alreadyIn) {
            //是否为<空>;
            checkToken(";");
        }

        addToResult("<语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoReturnStatement() {
        nextToken();

        if (is("(")) {
            if (inExpressionSelection()) {
                gotoExpression();
            } else {
                throwError();
            }

            checkToken(")");
        }

        addToResult("<返回语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoWriteStatement() {
        nextToken();

        checkToken("(");
        //防止标识符与字符串混淆
        //防止字符和字符串混淆
        if (!identifiersFromLA.contains(token) && !categoryCodeFromLA.get(pointer).equals("CHARCON") && inStringSeclecion()) {
            gotoString();

            if (is(",")) {
                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }
            }

            checkToken(")");
        } else if (inExpressionSelection()) {
            gotoExpression();

            checkToken(")");
        } else {
            throwError();
        }

        addToResult("<写语句>", true);
    }

    private void gotoString() {
        nextToken();

        addToResult("<字符串>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoReadStatement() {
        nextToken();

        checkToken("(");

        do {
            if (inIdentifierSelection()) {
                gotoIdentifier();
            } else {
                throwError();
            }
        } while (is(","));

        checkToken(")");

        addToResult("<读语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoValuation() {
        if (inIdentifierSelection()) {
            gotoIdentifier();

            if (is("=")) {
                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }
            } else if (is("[")) {
                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }

                checkToken("]");
                checkToken("=");

                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }
            }
        } else {
            throwError();
        }

        addToResult("<赋值语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoNoReturnValueCallStatement() {
        //<无返回值函数调用语句>和<有返回值函数调用语句>产生式相同
        gotoHasReturnValueCallStatement(true);
        //出现混淆之后需要直接返回
        if (needRetreat) {
            preToken(retreatStep);
            needRetreat = false;
            deleteWrongItemFromResult();
            return;
        }

//        addToResult("<无返回值函数调用语句>", true);
    }

    private void checkToken(String target) {
        if (token.equals(target)) {
            nextToken();
        } else {
            throwError();
        }
    }

    private boolean is(String target) {
        if (token.equals(target)) {
            nextToken();

            return true;
        }

        return false;
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoHasReturnValueCallStatement(boolean another) {
        boolean imActuallyVoid = false;
        //如果与<无返回值函数调用>语句发生了混淆
        if (voidTypeheaders.contains(token)) {
            //在<语句>中由于先判断<有返回值函数调用>并且<无返回值函数调用>和<有返回值函数调用>的产生式相同
            // 因此如果进入的是一个<无返回值函数调用>则需要进行特殊处理
            imActuallyVoid = true;
        }

        if (inIdentifierSelection()) {
            gotoIdentifier();
        } else {
            throwError();
        }
        //如果出现了标识符-方法头混淆,则需要先前回溯一个输入符并直接返回
        if (!is("(")) {
            needRetreat = true;
            setRetreatStep(1);
            return;
        }

        if (inValueParameterTableSelection()) {
            gotoValueParameterTable();
        } else {
            throwError();
        }

        checkToken(")");

        if (!another && !imActuallyVoid) {
            addToResult("<有返回值函数调用语句>", true);
        } else {
            addToResult("<无返回值函数调用语句>", true);
        }
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoValueParameterTable() {
        while (inExpressionSelection()) {
            gotoExpression();

            if (token.equals(",")) {
                nextToken();
            } else {
                break;
            }
        }

        addToResult("<值参数表>", true);
    }

    private void gotoLoopStatement() {
        //由于以终结符开头,所以需要先取下一个输入符
//        nextToken();

        //while循环
        if (is("while")) {
            checkToken("(");

            if (inConditionSelection()) {
                gotoCondition();

                checkToken(")");

                if (inStatementSelection()) {
                    gotoStatement();
                } else {
                    throwError();
                }
            }
            //for循环
        } else if (is("for")) {
            checkToken("(");

            if (inIdentifierSelection()) {
                gotoIdentifier();

                checkToken("=");

                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }

                checkToken(";");

                if (inConditionSelection()) {
                    gotoCondition();
                } else {
                    throwError();
                }

                checkToken(";");

                if (inIdentifierSelection()) {
                    gotoIdentifier();
                } else {
                    throwError();
                }

                checkToken("=");

                if (inIdentifierSelection()) {
                    gotoIdentifier();
                } else {
                    throwError();
                }

                if (inAddingOperatorSelection()) {
                    nextToken();
                } else {
                    throwError();
                }

                if (inStepValueSelection()) {
                    gotoStepValue();
                } else {
                    throwError();
                }

                checkToken(")");

                if (inStatementSelection()) {
                    gotoStatement();
                } else
                    throwError();
            } else {
                throwError();
            }
        } else if (is("do")) {
            if (inStatementSelection()) {
                gotoStatement();

                checkToken("while");
                checkToken("(");

                if (inConditionSelection()) {
                    gotoCondition();
                } else {
                    throwError();
                }

                checkToken(")");
            }
        } else {
            throwError();
        }

        addToResult("<循环语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoStepValue() {
        if (inUnsignIntegerSelection()) {
            gotoUnsignInteger();
        } else {
            throwError();
        }

        addToResult("<步长>", true);
    }

    private void gotoConditionalStatement() {
        //取下一个输入符
        nextToken();
        //该方法会调用nextToken()
        checkToken("(");

        if (inConditionSelection()) {
            gotoCondition();
        } else {
            throwError();
        }

        checkToken(")");

        if (inStatementSelection()) {
            gotoStatement();
        } else {
            throwError();
        }
        //如果有else输入符
        if (is("else")) {
            if (inStatementSelection()) {
                gotoStatement();
            } else {
                throwError();
            }
        }

        addToResult("<条件语句>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoCondition() {
        if (inExpressionSelection()) {
            gotoExpression();
        } else {
            throwError();
        }
        //如果下一个输入符属于<关系运算符>SELECT集合
        if (inRelationalOperatorSelection()) {
            //那么继续取一个输入符
            nextToken();
            //继续判断是否始于<表达式>SELECT集合
            if (inExpressionSelection()) {
                //进入<表达式>产生式
                gotoExpression();
            } else {
                throwError();
            }
        }

        addToResult("<条件>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoExpression() {
        //'+'和加法运算符可能混淆
        if (!categoryCodeFromLA.get(pointer).equals("CHARCON") && inAddingOperatorSelection()) {
            nextToken();
        }

        if (inItemSelection()) {
            gotoItem();
        }
        //闭包
        while (inAddingOperatorSelection()) {
            //忽略加法运算符,取下一个输入符
            nextToken();

            if (inItemSelection()) {
                gotoItem();
            } else
                throwError();
        }

        addToResult("<表达式>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoItem() {
        boolean firstIn = true;
        //正闭包
        do {
            //如果不是第一次循环则需要取下一个输入符
            // 因为判断是否属于<乘法操作符>SELECT集合之后,需要取下一个输入符
            if (!firstIn) nextToken();

            gotoFactor();

            firstIn = false;
        } while (inMultiplyOpertairSelection());

        addToResult("<项>", true);
    }

    /**
     * 返回后指针已经指向下一个输入符
     */
    private void gotoFactor() {
        boolean alreadyIn = false;

        if (inIdentifierSelection()) {
            gotoIdentifier();

            if (is("[")) {
                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }

                checkToken("]");
            }
            //如果是<有返回值的函数调用语句>则需要将指针回溯到标识符
            if (is("(")) {
                preToken(2);

                deleteWrongItemFromResult(2);
            } else {
                alreadyIn = true;
            }
        }

        if (!alreadyIn) {
            if (is("(")) {
                if (inExpressionSelection()) {
                    gotoExpression();
                } else {
                    throwError();
                }

                checkToken(")");
                //防止'+'和加法运算符混淆
            } else if (!categoryCodeFromLA.get(pointer).equals("CHARCON") && inIntegerSelection()) {
                gotoInteger();
            } else if (token.length() == 1 && inCharsSelection()) gotoChars();
            else if (inHasReturnValueCallStatementSelection()) {
                gotoHasReturnValueCallStatement(false);
            } else {
                throwError();
            }
        }

        addToResult("<因子>", true);
    }

    /**
     * 获取下一个单词(输入符)
     */
    private void nextToken() {
        if (pointer < wordsFromLA.size() - 1) {
            token = wordsFromLA.get(++pointer);
            addToResult(categoryCodeFromLA.get(pointer) + " " + token);
        } else {
            inTheEnd = true;
        }
    }

    private boolean inAddingOperatorSelection() {
        return token.equals("+") || token.equals("-");
    }

    private boolean inMultiplyOpertairSelection() {
        return token.equals("*") || token.equals("/");
    }

    private boolean inRelationalOperatorSelection() {
        return token.equals("<") || token.equals("<=") || token.equals(">") || token.equals(">=") ||
                token.equals("!=") || token.equals("==");
    }

    private boolean inAlphabetSelection() {
        //因为在所有产生式中需要调用该方法的情况中,token只可能有一个字符
        // 所以直接取token的第一个字符即可
        char ch = token.charAt(0);

        return ch == '_' || ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122;
    }

    private boolean inNumericalSelection() {
        //因为在所有产生式中需要调用该方法的情况中,token只可能有一个字符
        // 所以直接取token的第一个字符即可
        return token.charAt(0) == 48 || inNoneZeroNumericalSelection();
    }

    private boolean inNoneZeroNumericalSelection() {
        //因为在所有产生式中需要调用该方法的情况中,token只可能有一个字符
        // 所以直接取token的第一个字符即可
        char ch = token.charAt(0);

        return ch >= 49 && ch <= 57;
    }

    private boolean inCharsSelection() {
        return inAddingOperatorSelection() || inMultiplyOpertairSelection() || inAlphabetSelection() || inNumericalSelection();
    }

    private boolean inStringSeclecion() {
        //TODO: 未确定是否需要实现
        return true;
    }

    private boolean inConstantDeclarationSelection() {
        return token.equals("const");
    }

    private boolean inConstantDefineSelection() {
        return inTypeIdentifierSelection();
    }

    private boolean inVaribleDeclarationSelection() {
        return inVaribleDefineSelection();
    }

    private boolean inVaribleDefineSelection() {
        return inTypeIdentifierSelection();
    }

    private boolean inTypeIdentifierSelection() {
        return token.equals("int") || token.equals("char");
    }

    private boolean inUnsignIntegerSelection() {
        return inNoneZeroNumericalSelection() || token.equals("0");
    }

    private boolean inIntegerSelection() {
        return inAddingOperatorSelection() || inUnsignIntegerSelection();
    }

    private boolean inIdentifierSelection() {
        return inAlphabetSelection();
    }

    private boolean inAssignHeaderSelection() {
        return inTypeIdentifierSelection();
    }

    private boolean inReturnTypeMethodSelection() {
        return inAssignHeaderSelection();
    }

    private boolean inVoidMethodSelection() {
        return token.equals("void") && !wordsFromLA.get(pointer + 1).equals("main");
    }

    private boolean inCompoundStatementSelection() {
        return inConstantDeclarationSelection() || inVaribleDeclarationSelection() || inStatementColumnSelection();
    }

    private boolean inStatementColumnSelection() {
        return inStatementSelection();
    }

    private boolean inStatementSelection() {
        return inConditionalStatementSelection() || inLoopStatementSelection() || token.equals("{") ||
                inHasReturnValueCallStatementSelection() || inNoReturnValueCallStatementSelection() ||
                inValuationSelection() || inReadStatementSelection() || inWriteStatementSelection() ||
                token.equals(";") || inReturnStatementSelection();
    }

    private boolean inConditionalStatementSelection() {
        return token.equals("if");
    }

    private boolean inLoopStatementSelection() {
        return token.equals("while") || token.equals("do") || token.equals("for");
    }

    private boolean inHasReturnValueCallStatementSelection() {
        return inIdentifierSelection();
    }

    private boolean inNoReturnValueCallStatementSelection() {
        return inIdentifierSelection();
    }

    private boolean inValuationSelection() {
        return inIdentifierSelection();
    }

    private boolean inReadStatementSelection() {
        return token.equals("scanf");
    }

    private boolean inWriteStatementSelection() {
        return token.equals("printf");
    }

    private boolean inReturnStatementSelection() {
        return token.equals("return");
    }

    private boolean inParameterListSelection() {
        //FIRST / {ε} ∪ FOLLOW
        return inTypeIdentifierSelection() || token.equals(")");
    }

    private boolean inMainSelection() {
        return token.equals("void");
    }

    private boolean inFactorSelection() {
        return inIdentifierSelection() || token.equals("(") || inIntegerSelection() || inCharsSelection() ||
                inHasReturnValueCallStatementSelection();
    }

    private boolean inItemSelection() {
        return inFactorSelection();
    }

    private boolean inExpressionSelection() {
        return inAddingOperatorSelection() || inItemSelection();
    }

    private boolean inConditionSelection() {
        return inExpressionSelection();
    }

    private boolean inStepValueSelection() {
        return inUnsignIntegerSelection();
    }

    private boolean inValueParameterTableSelection() {
        return inExpressionSelection() || token.equals(")");
    }

    private void throwError() {
        //TODO: 1.抛出一个错误,希望能够直接中止程序!
    }
}