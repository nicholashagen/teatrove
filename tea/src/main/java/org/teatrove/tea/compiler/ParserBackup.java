/* ====================================================================
 * Tea - Copyright (c) 1997-2000 Walt Disney Internet Group
 * ====================================================================
 * The Tea Software License, Version 1.1
 *
 * Copyright (c) 2000 Walt Disney Internet Group. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Walt Disney Internet Group (http://opensource.go.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Tea", "TeaServlet", "Kettle", "Trove" and "BeanDoc" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact opensource@dig.com.
 *
 * 5. Products derived from this software may not be called "Tea",
 *    "TeaServlet", "Kettle" or "Trove", nor may "Tea", "TeaServlet",
 *    "Kettle", "Trove" or "BeanDoc" appear in their name, without prior
 *    written permission of the Walt Disney Internet Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE WALT DISNEY INTERNET GROUP OR ITS
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * For more information about Tea, please see http://opensource.go.com/.
 */

package org.teatrove.tea.compiler;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import org.teatrove.tea.annotations.NameParser;
import org.teatrove.tea.annotations.StatementParser;
import org.teatrove.tea.annotations.TemplateParser;
import org.teatrove.tea.annotations.TypeNameParser;
import org.teatrove.tea.modules.core.ClassTokens;
import org.teatrove.tea.modules.core.ConditionalTokens;
import org.teatrove.tea.modules.core.CoreTokens;
import org.teatrove.tea.modules.core.DefinitionTokens;
import org.teatrove.tea.modules.core.ForeachTokens;
import org.teatrove.tea.modules.core.ImportTokens;
import org.teatrove.tea.modules.core.InvokeTokens;
import org.teatrove.tea.modules.core.LambdaTokens;
import org.teatrove.tea.modules.core.LiteralTokens;
import org.teatrove.tea.modules.core.LoopTokens;
import org.teatrove.tea.modules.core.MathTokens;
import org.teatrove.tea.modules.core.RangeTokens;
import org.teatrove.tea.modules.core.StringTokens;
import org.teatrove.tea.modules.core.SubstitutionTokens;
import org.teatrove.tea.modules.core.TemplateTokens;
import org.teatrove.tea.parsetree.AndExpression;
import org.teatrove.tea.parsetree.ArithmeticExpression;
import org.teatrove.tea.parsetree.ArrayLookup;
import org.teatrove.tea.parsetree.AssignmentStatement;
import org.teatrove.tea.parsetree.Block;
import org.teatrove.tea.parsetree.BooleanLiteral;
import org.teatrove.tea.parsetree.BreakStatement;
import org.teatrove.tea.parsetree.ConcatenateExpression;
import org.teatrove.tea.parsetree.ContinueStatement;
import org.teatrove.tea.parsetree.DynamicTypeName;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.ExpressionList;
import org.teatrove.tea.parsetree.ExpressionStatement;
import org.teatrove.tea.parsetree.ForeachStatement;
import org.teatrove.tea.parsetree.FunctionCallExpression;
import org.teatrove.tea.parsetree.IfStatement;
import org.teatrove.tea.parsetree.ImportDirective;
import org.teatrove.tea.parsetree.LambdaStatement;
import org.teatrove.tea.parsetree.Lookup;
import org.teatrove.tea.parsetree.Name;
import org.teatrove.tea.parsetree.NegateExpression;
import org.teatrove.tea.parsetree.NewArrayExpression;
import org.teatrove.tea.parsetree.NewClassExpression;
import org.teatrove.tea.parsetree.NotExpression;
import org.teatrove.tea.parsetree.NullLiteral;
import org.teatrove.tea.parsetree.NumberLiteral;
import org.teatrove.tea.parsetree.OrExpression;
import org.teatrove.tea.parsetree.ParenExpression;
import org.teatrove.tea.parsetree.RelationalExpression;
import org.teatrove.tea.parsetree.Statement;
import org.teatrove.tea.parsetree.StatementList;
import org.teatrove.tea.parsetree.StringLiteral;
import org.teatrove.tea.parsetree.SubstitutionStatement;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.TemplateCallExpression;
import org.teatrove.tea.parsetree.TemplateClass;
import org.teatrove.tea.parsetree.TernaryExpression;
import org.teatrove.tea.parsetree.TypeName;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.tea.parsetree.VariableRef;
import org.teatrove.trove.classfile.Modifiers;
import org.teatrove.trove.io.SourceReader;

/******************************************************************************
 * A Parser creates the parse tree for a template by reading tokens emitted by
 * a {@link Scanner}. The parse tree represents the entire template as a
 * data structure composed of specialized nodes. Add an {@link ErrorListener}
 * to capture any syntax errors detected by the Parser.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision:--> 66 <!-- $-->, <!--$$JustDate:--> 11/14/03 <!-- $-->
 */
public class ParserBackup {
    private Scanner mScanner;
    private Annotations mAnnotations;
    private CompilationUnit mUnit;

    private Vector mListeners = new Vector(1);
    private int mErrorCount = 0;
    private int mEOFErrorCount = 0;

    private MessageFormatter mFormatter;

    public ParserBackup(Scanner scanner, Annotations annotations) {
        this(scanner, annotations, null);
    }

    public ParserBackup(Scanner scanner, Annotations annotations,
                  CompilationUnit unit) {
        mScanner = scanner;
        mAnnotations = annotations;
        mUnit = unit;
        mFormatter = MessageFormatter.lookup(this);
    }

    public void addErrorListener(ErrorListener listener) {
        mListeners.addElement(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        mListeners.removeElement(listener);
    }

    private void dispatchParseError(ErrorEvent e) {
        mErrorCount++;

        synchronized (mListeners) {
            for (int i = 0; i < mListeners.size(); i++) {
                ((ErrorListener)mListeners.elementAt(i)).compileError(e);
            }
        }
    }

    private void error(String str, Token culprit) {
        str = mFormatter.format(str);

        if (culprit.getID() == Tokens.EOF) {
            if (mEOFErrorCount++ == 0) {
                str = mFormatter.format("error.at.end", str);
            }
            else {
                return;
            }
        }

        dispatchParseError(new ErrorEvent(this, str, culprit, mUnit));
    }

    private void error(String str, String arg, Token culprit) {
        str = mFormatter.format(str, arg);

        if (culprit.getID() == Tokens.EOF) {
            if (mEOFErrorCount++ == 0) {
                str = mFormatter.format("error.at.end", str);
            }
            else {
                return;
            }
        }

        dispatchParseError(new ErrorEvent(this, str, culprit, mUnit));
    }

    private void error(String str, SourceInfo info) {
        str = mFormatter.format(str);
        dispatchParseError(new ErrorEvent(this, str, info, mUnit));
    }

    /**
     * Returns a parse tree by its root node. The parse tree is generated
     * from tokens read from the scanner. Any errors encountered while
     * parsing are delivered by dispatching an event. Add an error listener
     * in order to capture parse errors.
     *
     * @return Non-null template node, even if there were errors during
     * parsing.
     * @see ParserBackup#addErrorListener
     */
    public Template parse() throws IOException {
        Template t = parseTemplate();

        if (t != null) {
            return t;
        }

        return new Template(new SourceInfo(0, 0, 0), null, null, false, null, null);
    }

    public int getErrorCount() {
        return mErrorCount;
    }

    private Token read() throws IOException {
        return mScanner.readToken();
    }

    private Token peek() throws IOException {
        return mScanner.peekToken();
    }

    private void unread(Token token) throws IOException {
        mScanner.unreadToken(token);
    }

    // sort by priority
    // sort by interface dependencies
    // sort by before, after, around

    // on method invoke
        // all implementing intergaces
        // sort by before, around, impl, after

    // load all @Parser
    // new ParserDefinition()
    //


    private Template parseTemplate() throws IOException {

        // top node parse(chain)
            // chain = createChain(TemplateParser.class)
            // chain.parseParam();
                // chain.next()
            // parseParam(chain)


        // TemplateParser
            // parseTemplate(Chain<TemplateParser> chain, Scanner scanner)
                // parseKeyword(Core.TEMPLATE, scanner)
                // parseToken(Tokens.IDENT, scanner)
                // createChain(this).next().parseParameters();

            // parseParameter(Chain<TemplateParser> chain, Scanner scanner)
                // scanner.readToken()
                // chain.next().parseParameter()


        // ImportParser
            // @Before(TemplateParser.class)
            // parseTemplate(Scanner) {
                // parseImports(scanner)

            // @Around(TemplateParser.class)
            // parseTemplate(TemplateParser delegate, Scanner s) {
                // do stuff
                // delegate.parseTemplate(s)
                // end

            // parseImports(Scanner)
                // while true
                    // parseImport
                    // if null, break

            // parseImport
                // parseKeyword(import)
                // parseIdent()


        Name name;

        Variable[] params = null;

        Token token = read();
        SourceInfo directiveInfo = token.getSourceInfo();

        // Process directives
        ArrayList directiveList = new ArrayList();
        while (token.getID() == ImportTokens.IMPORT) {
            directiveList.add(new ImportDirective(directiveInfo, parseTypeName().getName()));
            token = read();
            if (token.getID() == CoreTokens.SEMI)
                token = read();

        }

        SourceInfo templateInfo = token.getSourceInfo();

        Token typeToken = token;
        int type = token.getID();
        if (type != TemplateTokens.TEMPLATE && type != ClassTokens.CLASS) {
            int peek = peek().getID();
            if (token.getID() == Tokens.STRING &&
                (peek == TemplateTokens.TEMPLATE || peek == ClassTokens.CLASS)) {

                error("template.start", token);
                token = read();
            }
            else {
                error("template.declaration", token);
            }
        }

        SourceInfo nameInfo = peek().getSourceInfo();
        name = new Name(nameInfo, parseIdentifier());

        // TODO: if type is class, support getter, setter, default, etc blocks
        params = parseFormalParameters();

        // Check if a block is accepted as a parameter. Pattern is { ... }
        boolean subParam = false;
        token = peek();
        if (token.getID() == CoreTokens.LBRACE ||
            token.getID() == SubstitutionTokens.ELLIPSIS) {
            if (type == ClassTokens.CLASS) {
                error("template.substitution.unsupported", token);
            }

            if (token.getID() == SubstitutionTokens.ELLIPSIS) {
                error("template.substitution.lbrace", token);
            }
            else {
                read();
                token = peek();
            }

            if (token.getID() == SubstitutionTokens.ELLIPSIS) {
                read();
                token = peek();
                if (token.getID() == CoreTokens.RBRACE) {
                    read();
                    subParam = true;
                }
                else {
                    error("template.substitution.rbrace", token);
                }
            }
            else {
                error("template.substitution.ellipsis", token);
                if (token.getID() == CoreTokens.RBRACE) {
                    read();
                    subParam = true;
                }
            }
        }

        // Parse statements until end of file is reached.
        StatementList statementList;
        Vector v = new Vector(10, 0);

        SourceInfo info = peek().getSourceInfo();
        Statement statement = null;
        while (peek().getID() != Tokens.EOF) {
            if (peek().getID() == ImportTokens.IMPORT) {
                read();
                error("statement.misuse.import", peek());
            }
            else {
                statement = parseStatement();
            }
            v.addElement(statement);
        }

        if (statement != null) {
            info = info.setEndPosition(statement.getSourceInfo());
        }

        Statement[] statements = new Statement[v.size()];
        v.copyInto(statements);

        statementList = new StatementList(info, statements);

        templateInfo =
            templateInfo.setEndPosition(statementList.getSourceInfo());

        if (type == TemplateTokens.TEMPLATE) {
            return new Template(templateInfo, name, params, subParam,
                                statementList, directiveList);
        }
        else if (type == ClassTokens.CLASS) {
            return new TemplateClass(templateInfo, name, params,
                                     statementList, directiveList);
        }
        else {
            error("template.unsupported.type", typeToken);
            return null;
        }
    }

    private String parseIdentifier() throws IOException {
        Token token = read();
        if (token.getID() != Tokens.IDENT) {
            /*
            TODO: get keyword
            if (token.isReservedWord()) {
                error("identifier.reserved.word", token.getName(), token);
                return token.getName();
            }
            else {
                error("identifier.expected", token);
                return "";
            }
            */
        }

        return token.getStringValue();
    }

    private Name parseName() throws IOException {
        SourceInfo info = null;
        StringBuffer name = new StringBuffer(20);

        while (true) {
            Token token = read();
            if (token.getID() != Tokens.IDENT) {
                if (info == null) {
                    info = token.getSourceInfo();
                }
                else {
                    info = info.setEndPosition(token.getSourceInfo());
                }

                /*
                TODO: look up in keywords
                if (token.isReservedWord()) {
                    error("name.reserved.word", token.getName(), token);
                    name.append(token.getName());
                }
                else {
                    error("name.identifier.expected", token);
                    break;
                }
                */
            }
            else {
                name.append(token.getStringValue());
                if (info == null) {
                    info = token.getSourceInfo();
                }
                else {
                    info = info.setEndPosition(token.getSourceInfo());
                }
            }

            token = peek();
            if (token.getID() != CoreTokens.DOT) {
                break;
            }
            else {
                token = read();
                name.append('.');
                info = info.setEndPosition(token.getSourceInfo());
            }
        }

        return new Name(info, name.toString());
    }

    private TypeName parseTypeName() throws IOException {
        // parse class name
        Name name = parseName();
        SourceInfo info = name.getSourceInfo();

        // parse generics
        List<TypeName> genericTypes = null;
        if (peek().getID() == Token.LT) {
            read();
            genericTypes = new ArrayList<TypeName>();
            while (true) {
                genericTypes.add(parseTypeName());

                Token token = read();
                if (token.getID() != Token.COMMA) {
                    if (token.getID() != Token.GT) {
                        error("name.generics.gt", token);
                    }

                    info = info.setEndPosition(token.getSourceInfo());
                    break;
                }
            }
        }

        // parse dimensions
        int dim = 0;
        while (peek().getID() == CoreTokens.LBRACK) {
            dim++;
            Token token = read(); // read the left bracket
            if (peek().getID() == CoreTokens.RBRACK) {
                token = read(); // read the right bracket
            }
            else {
                error("name.rbracket", peek());
            }
            info = info.setEndPosition(token.getSourceInfo());
        }

        if (genericTypes == null) {
            return new TypeName(info, name, dim);
        }
        else {
            TypeName[] generics =
                genericTypes.toArray(new TypeName[genericTypes.size()]);
            return new TypeName(info, name, generics, dim);
        }
    }

    private Variable parseVariableDeclaration(boolean isStaticallyTyped) throws IOException {
        Token token = peek();
        boolean dynamic = false;
        if (token.getID() == LiteralTokens.HASH) {
            read();
            dynamic = true;
        }

        TypeName typeName = parseTypeName();
        if (dynamic) {
            typeName = new DynamicTypeName(typeName);
        }

        SourceInfo info = peek().getSourceInfo();
        String varName = parseIdentifier();

        return new Variable(info, varName, typeName, isStaticallyTyped);
    }

    private Variable[] parseFormalParameters() throws IOException {
        Token token = peek();

        if (token.getID() == CoreTokens.LPAREN) {
            read(); // read the left paren
            token = peek();
        }
        else {
            error("params.lparen", token);
        }

        Vector vars = new Vector(10, 0);

        if (token.getID() == CoreTokens.RPAREN) {
            // Empty list detected.
        }
        else {
            while (true) {
                if ((token = peek()).getID() == CoreTokens.RPAREN) {
                    error("params.premature.end", token);
                    break;
                }

                vars.addElement(parseVariableDeclaration(false));

                if ((token = peek()).getID() != CoreTokens.COMMA) {
                    break;
                }
                else {
                    read(); // read the comma
                }
            }
        }

        if (token.getID() == CoreTokens.RPAREN) {
            read(); // read the right paren
        }
        else {
            error("params.rparen.expected", token);
        }

        Variable[] variables = new Variable[vars.size()];
        vars.copyInto(variables);

        return variables;
    }

    private VariableRef parseLValue() throws IOException {
        return parseLValue(read());
    }

    private VariableRef parseLValue(Token token) throws IOException {
        String loopVarName;
        if (token.getID() != Tokens.IDENT) {
            loopVarName = null;
            /*
            TODO: lookup in keywords
            if (token.isReservedWord()) {
                error("lvalue.reserved.word", token.getName(), token);
                loopVarName = token.getName();
            }
            else {
                error("lvalue.identifier.expected", token);
                loopVarName = "";
            }
            */
        }
        else {
            loopVarName = token.getStringValue();
        }

        return new VariableRef(token.getSourceInfo(), loopVarName);
    }

    private Block parseBlock() throws IOException {
        Token token = peek();
        SourceInfo info = token.getSourceInfo();

        if (token.getID() != CoreTokens.LBRACE) {
            error("block.lbrace.expected", token);
            if (token.getID() == CoreTokens.SEMI) {
                read();
                return new Block(info, new Statement[0]);
            }
        }
        else {
            token = read(); // read the left brace
        }

        Vector v = new Vector(10, 0);
        Token p;
        while ((p = peek()).getID() != CoreTokens.RBRACE) {
            if (p.getID() == Tokens.EOF) {
                error("block.rbrace.expected", p);
                break;
            }
            v.addElement(parseStatement());
        }
        token = read(); // read the right brace

        Statement[] statements = new Statement[v.size()];
        v.copyInto(statements);

        info = info.setEndPosition(token.getSourceInfo());

        return new Block(info, statements);
    }

    private Statement parseStatement() throws IOException {
        Statement st = null;

        while (st == null) {
            Token token = read();

            if (token.getID() == CoreTokens.SEMI) {
                // If the token after the semi-colon is a right brace,
                // we can't simply skip it because this method
                // can't properly parse a right brace. Instead, return
                // an empty placeholder statement. The parseBlock method
                // will then be able to parse the right brace properly.

                int ID = peek().getID();
                if (ID == CoreTokens.RBRACE || ID == Tokens.EOF) {
                    st = new Statement(token.getSourceInfo());
                }
                else {
                    // Skip this token
                }
            }
            else if (token.getID() == LoopTokens.BREAK) {
                st = parseBreakStatement(token);
            }
            else if (token.getID() == LoopTokens.CONTINUE) {
                st = parseContinueStatement(token);
            }
            else if (token.getID() == ConditionalTokens.IF) {
                st = parseIfStatement(token);
            }
            else if (token.getID() == ForeachTokens.FOREACH) {
                st = parseForeachStatement(token);
            }
            else if (token.getID() == DefinitionTokens.DEFINE) {
                SourceInfo info = token.getSourceInfo();
                Variable v = parseVariableDeclaration(true);
                VariableRef lvalue = new VariableRef(info, v.getName());
                lvalue.setVariable(v);
                Expression rvalue = new NullLiteral(info);   // Empty expression is null assignment

                if (peek().getID() == CoreTokens.ASSIGN) {
                    read();
                    rvalue = parseExpression();
                    info = info.setEndPosition(rvalue.getSourceInfo());
                }

                st = new AssignmentStatement(info, lvalue, rvalue);
            }
            else if (token.getID() == Tokens.IDENT) {
                if (peek().getID() == CoreTokens.ASSIGN) {
                    st = parseAssignmentStatement(token);
                }
                else {
                    st = new ExpressionStatement(parseExpression(token));
                }
            }
            else if (token.getID() == SubstitutionTokens.ELLIPSIS) {
                st = new SubstitutionStatement(token.getSourceInfo());
            }
            else if (token.getID() == Tokens.EOF) {
                error("statement.expected", token);
                st = new Statement(token.getSourceInfo());
            }

            // Handle some error cases in a specialized way so that
            // the error message produced is more meaningful.
            else if (token.getID() == ConditionalTokens.ELSE) {
                error("statement.misuse.else", token);
                st = parseBlock();
            }
            else if (token.getID() == ForeachTokens.IN) {
                error("statement.misuse.in", token);
                st = new ExpressionStatement(parseExpression(token));
            }

            else if (token.getID() == ForeachTokens.REVERSE) {
                error("statement.misuse.reverse", token);
                st = new ExpressionStatement(parseExpression(token));
            }

            else {
                st = new ExpressionStatement(parseExpression(token));
            }
        }

        return st;
    }

    // When this is called, the keyword "break" has already been read.
    private BreakStatement parseBreakStatement(Token token)
        throws IOException {
        return new BreakStatement(token.getSourceInfo());
    }

    // When this is called, the keyword "continue" has already been read.
    private ContinueStatement parseContinueStatement(Token token)
        throws IOException {
        return new ContinueStatement(token.getSourceInfo());
    }

    // When this is called, the keyword "if" has already been read.
    private IfStatement parseIfStatement(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();

        Expression condition = parseExpression();

        if (!(condition instanceof ParenExpression)) {
            error("if.condition", condition.getSourceInfo());
        }

        Block thenPart = parseBlock();
        Block elsePart = null;

        token = peek();
        if (token.getID() != ConditionalTokens.ELSE) {
            info = info.setEndPosition(thenPart.getSourceInfo());
        }
        else {
            read(); // read the else keyword
            token = peek();
            if (token.getID() == ConditionalTokens.IF) {
                elsePart = new Block(parseIfStatement(read()));
            }
            else {
                elsePart = parseBlock();
            }

            info = info.setEndPosition(elsePart.getSourceInfo());
        }

        return new IfStatement(info, condition, thenPart, elsePart);
    }

    // When this is called, the keyword "foreach" has already been read.
    private ForeachStatement parseForeachStatement(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();

        token = peek();
        if (token.getID() == CoreTokens.LPAREN) {
            read();
        }
        else {
            error("foreach.lparen.expected", token);
        }

        VariableRef loopVar = parseLValue();

        // mod for declarative typing
        boolean foundASToken = false;
        Token asToken = peek();
        if (asToken.getID() == DefinitionTokens.AS) {
            foundASToken = true;
            read();
            TypeName typeName = parseTypeName();
            SourceInfo info2 = peek().getSourceInfo();
            loopVar.setVariable(new Variable(info2, loopVar.getName(), typeName, true));
        }
        // end mod

        token = peek();
        if (token.getID() == ForeachTokens.IN) {
            read();
        }
        else {
            error("foreach.in.expected", token);
        }

        Expression range = parseExpression();
        Expression endRange = null;

        token = peek();
        if (token.getID() == RangeTokens.DOTDOT) {
            read();
            endRange = parseExpression();
            token = peek();
        }

        if (endRange != null && foundASToken)
            error("foreach.as.not.allowed", asToken);

        boolean reverse = false;
        if (token.getID() == ForeachTokens.REVERSE) {
            read();
            reverse = true;
            token = peek();
        }

        if (token.getID() == CoreTokens.RPAREN) {
            read();
        }
        else {
            error("foreach.rparen.expected", token);
        }

        Block body = parseBlock();

        info = info.setEndPosition(body.getSourceInfo());

        return new ForeachStatement
            (info, loopVar, range, endRange, reverse, body);
    }

    // When this is called, the identifier token has already been read.
    private AssignmentStatement parseAssignmentStatement(Token token)
        throws IOException {

        // TODO: allow lvalue to support dot notations
        // ie: field = x (store local variable)
        //     obj.field = x (obj.setField)
        //     obj.field.field = x (obj.getField().setField)
        //     array[idx] = x (array[idx])
        //     list[idx] = x (list.set(idx, x))
        //     map[key] = x (map.put(key, x))
        //     map[obj.name] = x (map.put(obj.getName(), x)
        SourceInfo info = token.getSourceInfo();
        VariableRef lvalue = parseLValue(token);

        if (peek().getID() == CoreTokens.ASSIGN) {
            read();
        }
        else {
            error("assignment.equals.expected", peek());
        }

        Expression rvalue = parseExpression();

        info = info.setEndPosition(rvalue.getSourceInfo());

        // Start mod for 'as' keyword for declarative typing
        if (peek().getID() == DefinitionTokens.AS) {
            read();
            TypeName typeName = parseTypeName();
            SourceInfo info2 = peek().getSourceInfo();
            lvalue.setVariable(new Variable(info2, lvalue.getName(), typeName, true));
        }
        // End mod

        return new AssignmentStatement(info, lvalue, rvalue);
    }

    /**
     * @param bracketed True if the list is bounded by brackets instead of
     * parenthesis.
     * @param associative True if the list declares associative array values.
     */
    private ExpressionList parseList(int lbracket, int rbracket,
                                     boolean associative)
        throws IOException {

        int leftID = lbracket;
        int rightID = rbracket;

        /*
        if (!bracketed) {
            leftID = Tokens.LPAREN;
            rightID = Tokens.RPAREN;
        }
        else {
            leftID = Tokens.LBRACK;
            rightID = Tokens.RBRACK;
        }
        */

        Token token = peek();
        SourceInfo info = token.getSourceInfo();

        if (token.getID() == leftID) {
            read(); // read the left paren
            token = peek();
        }
        else {
            if (lbracket == CoreTokens.LPAREN) {
                error("list.lparen.expected", token);
            }
            else if (lbracket == CoreTokens.LBRACK) {
                error("list.lbracket.expected", token);
            }
            else if (lbracket == CoreTokens.LBRACE) {
                error("list.lbrace.expected", token);
            }
            else {
                error("list.expected", token);
            }
        }

        Vector exprs = new Vector(10, 0);
        boolean done = false;

        if (token.getID() == rightID) {
            // Empty list detected
        }
        else {
            Expression expr = null;
            while (true) {
                token = read();

                if (token.getID() == rightID) {
                    error("list.premature.end", token);
                    info = info.setEndPosition(token.getSourceInfo());
                    done = true;
                    break;
                }

                expr = parseExpression(token);
                exprs.addElement(expr);

                token = peek();

                if (token.getID() != CoreTokens.COMMA &&
                    token.getID() != LiteralTokens.EQUAL_GREATER &&
                    token.getID() != CoreTokens.COLON) {
                    break;
                }
                else {
                    token = read(); // read the comma or equal_greater separator
                }
            }

            if (!done && expr != null) {
                info = info.setEndPosition(expr.getSourceInfo());
            }
        }

        if (!done) {
            token = peek();

            if (token.getID() == rightID) {
                token = read(); // read the right paren
                info = info.setEndPosition(token.getSourceInfo());
            }
            else {
                if (rbracket == CoreTokens.RPAREN) {
                    error("list.rparen.expected", token);
                }
                else if (rbracket == CoreTokens.RBRACK) {
                    error("list.rbracket.expected", token);
                }
                else if (rbracket == CoreTokens.RBRACE) {
                    error("list.rbrace.expected", token);
                }
                else {
                    error("list.expected", token);
                }
            }
        }

        Expression[] elements = new Expression[exprs.size()];
        exprs.copyInto(elements);

        return new ExpressionList(info, elements);
    }

    private Expression parseExpression() throws IOException {
        return parseExpression(read());
    }

    private Expression parseExpression(Token token) throws IOException {
        return parseTernaryExpression(token);
    }

    private Expression parseTernaryExpression(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();
        Expression expr = parseOrExpression(token);

        token = peek();
        if (token.getID() == CoreTokens.QUESTION) {
            read();
            token = peek();
            if (token.getID() == CoreTokens.COLON) {
                read();

                Expression thenExpr = (Expression) expr.clone();
                Expression elseExpr = parseTernaryExpression(read());
                info = info.setEndPosition(elseExpr.getSourceInfo());
                expr = new TernaryExpression(info, expr, thenExpr, elseExpr);
            }
            else {
                Expression thenExpr = parseTernaryExpression(read());

                token = peek();
                if (token.getID() != CoreTokens.COLON) {
                    error("ternary.colon.expected", token);
                }

                read();

                Expression elseExpr = parseTernaryExpression(read());
                info = info.setEndPosition(elseExpr.getSourceInfo());
                expr = new TernaryExpression(info, expr, thenExpr, elseExpr);
            }
        }

        return expr;
    }

    private Expression parseOrExpression(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();
        Expression expr = parseAndExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == ConditionalTokens.OR) {
                read();
                Expression right = parseAndExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new OrExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseAndExpression(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();
        Expression expr = parseEqualityExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == ConditionalTokens.AND) {
                read();
                Expression right = parseEqualityExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new AndExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseEqualityExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseRelationalExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == CoreTokens.ASSIGN) {
                error("equality.misuse.assign", token);
                token = null; // TODO: fix - new Token(token.getSourceInfo(), ConditionalTokens.EQ);
            }

            if (token.getID() == ConditionalTokens.EQ ||
                token.getID() == ConditionalTokens.NE) {
                read();
                Expression right = parseRelationalExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseRelationalExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseConcatenateExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == ConditionalTokens.LT ||
                token.getID() == ConditionalTokens.GT ||
                token.getID() == ConditionalTokens.LE ||
                token.getID() == ConditionalTokens.GE) {
                read();
                Expression right = parseConcatenateExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, right);
            }
            else if (token.getID() == DefinitionTokens.ISA) {
                read();
                TypeName typeName = parseTypeName();
                info = info.setEndPosition(typeName.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, typeName);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseConcatenateExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseAdditiveExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == StringTokens.CONCAT) {
                read();
                Expression right = parseAdditiveExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new ConcatenateExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseAdditiveExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseMultiplicativeExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == MathTokens.PLUS ||
                token.getID() == MathTokens.MINUS) {
                read();
                Expression right = parseMultiplicativeExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new ArithmeticExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseMultiplicativeExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseUnaryExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == MathTokens.MULT ||
                token.getID() == MathTokens.DIV ||
                token.getID() == MathTokens.MOD) {
                read();
                Expression right = parseUnaryExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new ArithmeticExpression(info, token, expr, right);
            }
            else {
                break loop;
            }
        }

        return expr;
    }

    private Expression parseUnaryExpression(Token token) throws IOException {
        SourceInfo info;
        Expression expr;

        if (token.getID() == ConditionalTokens.NOT) {
            info = token.getSourceInfo();
            expr = parseUnaryExpression(read());
            info = info.setEndPosition(expr.getSourceInfo());
            return new NotExpression(info, expr);
        }
        else if (token.getID() == MathTokens.MINUS) {
            info = token.getSourceInfo();
            expr = parseUnaryExpression(read());
            info = info.setEndPosition(expr.getSourceInfo());
            return new NegateExpression(info, expr);
        }

        return parseLookup(token);
    }

    private Expression parseLookup(Token token) throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseFactor(token);

        while (true) {
            token = peek();

            if (token.getID() == CoreTokens.DOT) {
                // "dot" lookup i.e.: a.b

                Token dot = read(); // read the dot

                // TODO: check for null-safe operator (.?)
                // TODO: check for spread operator (.*)
                token = read();

                Name lookupName;
                SourceInfo nameInfo = token.getSourceInfo();
                if (token.getID() != Tokens.IDENT) {
                    lookupName = null;
                    /*
                    TODO: look in keywords
                    if (token.isReservedWord()) {
                        error("lookup.reserved.word", token.getName(), token);
                        lookupName = new Name(nameInfo, token.getName());
                    }
                    else {
                        error("lookup.identifier.expected", token);
                        lookupName = new Name(nameInfo, null);
                    }
                    */
                }
                else {
                    lookupName = new Name(nameInfo, token.getStringValue());
                    info = info.setEndPosition(nameInfo);
                }

                expr = new Lookup(info, expr, dot, lookupName);
            }
            else if (token.getID() == CoreTokens.LBRACK) {
                // array lookup i.e.: a[b]

                Token lbrack = read(); // read the left bracket

                token = read();

                if (token.getID() == CoreTokens.RBRACK) {
                    info = info.setEndPosition(token.getSourceInfo());

                    error("lookup.empty.brackets", token);

                    expr = new ArrayLookup(info, expr, lbrack,
                                           new Expression(info));

                    continue;
                }

                Expression arrayLookup = parseExpression(token);

                token = peek();

                if (token.getID() == CoreTokens.RBRACK) {
                    read(); // read the right bracket
                    info = info.setEndPosition(token.getSourceInfo());
                }
                else {
                    error("lookup.rbracket.expected", token);
                    info = info.setEndPosition(arrayLookup.getSourceInfo());
                }

                expr = new ArrayLookup(info, expr, lbrack, arrayLookup);
            }
            else {
                break;
            }
        }

        return expr;
    }

    private Expression parseFactor(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();

        if (token.getID() == LiteralTokens.HASH ||
            token.getID() == LiteralTokens.DOUBLE_HASH) {
            return parseNewArrayExpression(token);
        }
        else if (token.getID() == CoreTokens.LPAREN) {
            Expression expr;

            token = peek();
            if (token.getID() == CoreTokens.RPAREN) {
                expr = null;
            }
            else {
                expr = parseExpression(read());
            }

            token = peek();
            if (token.getID() == CoreTokens.RPAREN) {
                read(); // read the right paren
                info = info.setEndPosition(token.getSourceInfo());
            }
            else {
                error("factor.rparen.expected", token);
                info = info.setEndPosition(expr.getSourceInfo());
            }

            if (expr == null) {
                error("factor.empty.parens", info);
                expr = new Expression(info);
            }

            return new ParenExpression(info, expr);
        }
        else if (token.getID() == LiteralTokens.NULL) {
            return new NullLiteral(info);
        }
        else if (token.getID() == LiteralTokens.TRUE) {
            return new BooleanLiteral(info, true);
        }
        else if (token.getID() == LiteralTokens.FALSE) {
            return new BooleanLiteral(info, false);
        }
        else if (token.getID() == InvokeTokens.CALL) {
            Name target = parseName();
            info.setEndPosition(target.getSourceInfo());

            ExpressionList list =
                parseList(CoreTokens.LPAREN, CoreTokens.RPAREN, false);
            info = info.setEndPosition(list.getSourceInfo());

            // Check if a block is being passed in the call.
            Block subParam = null;
            if (peek().getID() == CoreTokens.LBRACE) {
                subParam = parseBlock();
                info = info.setEndPosition(subParam.getSourceInfo());
            }

            return new TemplateCallExpression(info, target, list, subParam);
        }
        else if (token.getID() == Tokens.NUMBER) {
            if (token.getNumericType() == 0) {
                error("factor.number.invalid", token);
            }

            switch (token.getNumericType()) {
            case 1:
                return new NumberLiteral(info, token.getIntValue());
            case 2:
                return new NumberLiteral(info, token.getLongValue());
            case 3:
                return new NumberLiteral(info, token.getFloatValue());
            case 4:
            default:
                return new NumberLiteral(info, token.getDoubleValue());
            }
        }
        else if (token.getID() == Tokens.STRING) {
            return new StringLiteral(info, token.getStringValue());
        }
        else if (token.getID() == Tokens.IDENT) {
            FunctionCallExpression call = parseFunctionCallExpression(token);
            if (call != null) {
                return call;
            }
            else {
                return new VariableRef(info, token.getStringValue());
            }
        }
        else if (token.getID() == Tokens.EOF) {
            error("factor.expression.expected", token);
        }
        else if (token.getID() == CoreTokens.RPAREN) {
            error("factor.rparen.unmatched", token);
        }
        else if (token.getID() == CoreTokens.RBRACE) {
            error("factor.rbrace.unmatched", token);
        }
        else if (token.getID() == CoreTokens.RBRACK) {
            error("factor.rbracket.unmatched", token);
        }
        else if (token.getID() == CoreTokens.ASSIGN) {
            error("factor.illegal.assignment", token);
        }
        else if (token.getID() == RangeTokens.DOTDOT) {
            error("factor.misuse.dotdot", token);
        }
        else {
            /*
            TODO: look up in keywords
            if (token.isReservedWord()) {
                error("factor.reserved.word", token.getName(), token);
            }
            else {
                error("factor.unexpected.token", token);
            }
            */
        }

        return new Expression(token.getSourceInfo());
    }

    private Expression parseNewArrayExpression(Token token)
        throws IOException {

        Name name = null;
        Token next = peek();
        if (next.getID() == Tokens.IDENT) {
            name = parseName();
        }

        boolean anonymous = (name == null && next.getID() == CoreTokens.LBRACE);
        boolean associative = (token.getID() == LiteralTokens.DOUBLE_HASH);

        SourceInfo info = token.getSourceInfo();
        ExpressionList list = null;
        if (name != null || anonymous) {
            list = parseList(CoreTokens.LBRACE, CoreTokens.RBRACE, associative);
        } else {
            list = parseList(CoreTokens.LPAREN, CoreTokens.RPAREN, associative);
        }

        info = info.setEndPosition(list.getSourceInfo());

        if (name != null || anonymous) {
            if (associative) {
                Expression[] exprs = list.getExpressions();
                for (int i = 0; i < exprs.length; i += 2) {
                    if (exprs[i] instanceof StringLiteral) {
                        continue;
                    } else if (exprs[i] instanceof VariableRef) {
                        SourceInfo source = exprs[i].getSourceInfo();
                        String varname = ((VariableRef) exprs[i]).getName();
                        exprs[i] = new StringLiteral(source, varname);
                    } else {
                        error("newclass.invalid.key", token);
                    }

                    list = new ExpressionList(list.getSourceInfo(), exprs);
                }
            }

            if (anonymous) {
                return new NewClassExpression(info, list);
            } else {
                return new NewClassExpression(info, name, list, associative);
            }
        }
        else {
            return new NewArrayExpression(info, list, associative);
        }
    }

    private LambdaStatement parseLambda(Block block)
        throws IOException {
        SourceInfo info = null;
        List<VariableRef> vars = new ArrayList<VariableRef>();
        while (true) {
            Token peek = peek();
            if (peek.getID() != Tokens.IDENT) {
                read();
                error("function.missing.identifer", peek);

                if (peek.getID() == LambdaTokens.LAMBDA) { break; }
                else { continue; }
            }

            if (info == null) {
                info = peek.getSourceInfo();
            }
            else {
                info = info.setEndPosition(peek.getSourceInfo());
            }

            Name ident = parseName();
            VariableRef var =
                new VariableRef(ident.getSourceInfo(), ident.getName());
            // parseVariableDeclaration(isStaticallyTyped);
            vars.add(var);

            Token next = read();
            if (next.getID() == CoreTokens.COMMA) {
                continue;
            }
            else if (next.getID() == LambdaTokens.LAMBDA) {
                break;
            }
        }

        return new LambdaStatement(info,
                                   vars.toArray(new VariableRef[vars.size()]),
                                   block);
    }

    // Special parse method in that it may return null if it couldn't parse
    // a FunctionCallExpression. Token passed in must be an identifier.
    private FunctionCallExpression parseFunctionCallExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();

        // Search for pattern <ident> {<dot> <ident>} <lparen>
        Vector lookahead = new Vector();
        StringBuffer name = new StringBuffer(token.getStringValue());
        Name target = null;

        while (true) {
            token = read();
            lookahead.addElement(token);

            if (token.getID() == CoreTokens.DOT) {
                name.append('.');
                info = info.setEndPosition(token.getSourceInfo());
            }
            else if (token.getID() == CoreTokens.LPAREN) {
                target = new Name(info, name.toString());
                unread(token);
                break;
            }
            else {
                break;
            }

            token = read();
            lookahead.addElement(token);

            if (token.getID() == Tokens.IDENT) {
                name.append(token.getStringValue());
                info = info.setEndPosition(token.getSourceInfo());
            }
            else {
                break;
            }
        }

        if (target == null) {
            // Pattern not found, unread all lookahead tokens.
            for (int i = lookahead.size() - 1; i >= 0; --i) {
                unread((Token)lookahead.elementAt(i));
            }
            return null;
        }

        ExpressionList list =
            parseList(CoreTokens.LPAREN, CoreTokens.RPAREN, false);
        info = info.setEndPosition(list.getSourceInfo());

        // Check if a block is being passed in the call.
        LambdaStatement lambda = null;
        Block subParam = null;
        if (peek().getID() == CoreTokens.LBRACE) {
            Token brace = read();

            // search for a lambda expression: ident,ident ->
            boolean found = false;
            lookahead.clear();
            while (true) {
                Token t = read();
                lookahead.add(t);
                if (t.getID() == Tokens.IDENT || t.getID() == CoreTokens.COMMA) {
                    continue;
                }
                else if (t.getID() == LambdaTokens.LAMBDA) {
                    found = true;
                    break;
                }
                else { break; }
            }

            // revert expressions and process next
            for (int i = lookahead.size() - 1; i >= 0; --i) {
                unread((Token)lookahead.elementAt(i));
            }

            // parse lamdbda expression
            if (found) {
                lambda = parseLambda(null);
            }

            // unread brace to parse block
            unread(brace);

            // process block
            subParam = parseBlock();
            info = info.setEndPosition(subParam.getSourceInfo());

            // update statement
            if (lambda != null) {
                lambda.setBlock(subParam);
                subParam = lambda;
            }
        }

        return new FunctionCallExpression(info, target, list, subParam);
    }

    public static Object createProxy(final Class<?> clazz) {
        return Enhancer.create(clazz, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {

                Method delegate = null;
                Class<?> current = clazz;
                while (delegate == null && current != null) {
                    try {
                        delegate = current.getDeclaredMethod
                        (
                            method.getName(), method.getParameterTypes()
                        );
                    }
                    catch (NoSuchMethodException nsme) {
                        current = current.getSuperclass();
                    }
                }

                if (delegate == null) {
                    /*
                    throw new UnsupportedOperationException
                    (
                        "unsupported method: " +
                        clazz.getName() + '.' + method.getName()
                    );
                    */
                    return null;
                }

                return delegate.invoke(proxy, args);
            }
        });
    }

    private static Map<Class<?>, ParserDefinition> mParsers =
        new HashMap<Class<?>, ParserDefinition>();

    public static void registerParser(String className)
        throws Exception {

        // load parser instance
        Class<?> clazz = Class.forName(className);
        if (mParsers.containsKey(clazz)) { return; }

        // create instance around parser
        Object instance = null;
        if ((clazz.getModifiers() & Modifiers.ABSTRACT) == 0) {
            Constructor<?> ctor = clazz.getConstructor();
            if (ctor == null) {
                throw new ParserDefinitionException
                (
                    clazz, "missing default constructor"
                );
            }

            instance = clazz.newInstance();
        }
        else {
            instance = createProxy(clazz);
        }

        // ParserDefnition
            // getParsers(type)

        // Parsers
            // getParsers(type)


        // foreach Parsers.getParsers(templateParser.class)
            // parserMethod.invoke(chain, parser)

        // parseName
            // Prsers.get(Name.class)
            // foreach (parserMethod.invoke(chain, parser);

        // ParserInvocation
            // definition / instance
            // method
            // annotation

    }

    /** Test program */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] arg) throws Exception {

        Annotations annotations = new Annotations();
        Scanner scanner = new Scanner
        (
            new SourceReader
            (
                new StringReader
                (
                    "<% template test(int id, String name) { ... } " +
                    "a = 5; b = a; ...; d = a + b; e = #(a, b); " +
                    "foreach (f in 1..5) { f } " +
                    "d " +
                    "%>"
                ), "<%", "%>"
            ),
            annotations
        );
        scanner.emitSpecialTokens(true);

        ParserBackup parser = new ParserBackup(scanner, annotations);
        Parsers parsers = new Parsers(annotations);
        ParserDefinition[] definitions = parsers.getParsers(TemplateParser.class);
        for (ParserDefinition definition : definitions) {

            Object result = null; // definition.invoke(new Chain(parsers, definition), parser);
            if (result != null) { /* return result; */ break; }
        }

        // on invoke
            // foreach
                // if @Intro
                    // invoke

        // chain.define
            // reset
            // implement interface
            // on first invoke, setup by method
            // invoke first tree

        // chain.next
            // invoke next in tree


        // load all parsers
        // organize
        // load first
        // proxy each parser
        // while true
            // invoke top of tree (by before/around, priority)
            // if null, next in tree

        // Tester.test(arg);
    }

    /**************************************************************************
     *
     * @author Brian S O'Neill
     * @version
     * <!--$$Revision:--> 66 <!-- $--> 36 <!-- $$JustDate:--> 11/14/03 <!-- $-->
     */
    private static class Tester implements ErrorListener {
        String mFilename;

        public static void test(String[] arg) throws Exception {
            new Tester(arg[0]);
        }

        public Tester(String filename) throws Exception {
            mFilename = filename;
            Reader file = new BufferedReader(new FileReader(filename));
            Scanner scanner = null; // new Scanner(new SourceReader(file, "<%", "%>"));
            scanner.addErrorListener(this);
            ParserBackup parser = null;//new Parser(scanner);
            parser.addErrorListener(this);
            Template tree = parser.parse();

            if (tree != null) {
                TreePrinter printer = new TreePrinter(tree);
                printer.writeTo(System.out);
            }
        }

        public void compileError(ErrorEvent e) {
            System.out.println(e.getDetailedErrorMessage());
        }
    }
}
