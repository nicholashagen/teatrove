/*
 *  Copyright 1997-2011 teatrove.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teatrove.tea.compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.teatrove.tea.parsetree.AndExpression;
import org.teatrove.tea.parsetree.ArithmeticExpression;
import org.teatrove.tea.parsetree.ArrayLookup;
import org.teatrove.tea.parsetree.AssignmentStatement;
import org.teatrove.tea.parsetree.Block;
import org.teatrove.tea.parsetree.BooleanLiteral;
import org.teatrove.tea.parsetree.BreakStatement;
import org.teatrove.tea.parsetree.CallExpression;
import org.teatrove.tea.parsetree.CompareExpression;
import org.teatrove.tea.parsetree.ConcatenateExpression;
import org.teatrove.tea.parsetree.ContinueStatement;
import org.teatrove.tea.parsetree.Directive;
import org.teatrove.tea.parsetree.DynamicTypeName;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.ExpressionList;
import org.teatrove.tea.parsetree.ExpressionStatement;
import org.teatrove.tea.parsetree.ForeachStatement;
import org.teatrove.tea.parsetree.FunctionCallExpression;
import org.teatrove.tea.parsetree.IfStatement;
import org.teatrove.tea.parsetree.ImportDirective;
import org.teatrove.tea.parsetree.LambdaBlock;
import org.teatrove.tea.parsetree.LambdaExpression;
import org.teatrove.tea.parsetree.LambdaStatement;
import org.teatrove.tea.parsetree.Lookup;
import org.teatrove.tea.parsetree.Name;
import org.teatrove.tea.parsetree.NegateExpression;
import org.teatrove.tea.parsetree.NewArrayExpression;
import org.teatrove.tea.parsetree.NewClassExpression;
import org.teatrove.tea.parsetree.Node;
import org.teatrove.tea.parsetree.NotExpression;
import org.teatrove.tea.parsetree.NullLiteral;
import org.teatrove.tea.parsetree.NullSafe;
import org.teatrove.tea.parsetree.NumberLiteral;
import org.teatrove.tea.parsetree.OrExpression;
import org.teatrove.tea.parsetree.ParenExpression;
import org.teatrove.tea.parsetree.RelationalExpression;
import org.teatrove.tea.parsetree.SpreadExpression;
import org.teatrove.tea.parsetree.Statement;
import org.teatrove.tea.parsetree.StatementList;
import org.teatrove.tea.parsetree.StringLiteral;
import org.teatrove.tea.parsetree.SubstitutionExpression;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.TemplateCallExpression;
import org.teatrove.tea.parsetree.TemplateClass;
import org.teatrove.tea.parsetree.TernaryExpression;
import org.teatrove.tea.parsetree.TypeName;
import org.teatrove.tea.parsetree.Variable;
import org.teatrove.tea.parsetree.VariableRef;
import org.teatrove.trove.io.SourceReader;

/**
 * A Parser creates the parse tree for a template by reading tokens emitted by
 * a {@link Scanner}. The parse tree represents the entire template as a
 * data structure composed of specialized nodes. Add an {@link ErrorListener}
 * to capture any syntax errors detected by the Parser.
 *
 * @author Brian S O'Neill
 */
public class Parser {
    private Scanner mScanner;
    private CompilationUnit mUnit;

    private Vector<ErrorListener> mListeners = new Vector<ErrorListener> (1);
    private int mErrorCount = 0;
    private int mEOFErrorCount = 0;

    private MessageFormatter mFormatter;

    public Parser(Scanner scanner) {
        this(scanner, null);
    }

    public Parser(Scanner scanner, CompilationUnit unit) {
        mScanner = scanner;
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

        if (culprit.getID() == Token.EOF) {
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

        if (culprit.getID() == Token.EOF) {
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
     * @see Parser#addErrorListener
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

    private Template parseTemplate() throws IOException {
        Name name;
        Variable[] params = null;

        Token token = read();
        SourceInfo directiveInfo = token.getSourceInfo();

        // Process directives
        ArrayList<Directive> directiveList = new ArrayList<Directive>();
        while (token.getID() == Token.IMPORT) {
            directiveList.add(
                new ImportDirective(directiveInfo, parseTypeName().getName())
            );
            token = read();
            if (token.getID() == Token.SEMI)
                token = read();

        }

        SourceInfo templateInfo = token.getSourceInfo();

        Token typeToken = token;
        int type = token.getID();
        if (type != Token.TEMPLATE && type != Token.CLASS) {
            int peek = peek().getID();
            if (token.getID() == Token.STRING &&
                (peek == Token.TEMPLATE || peek == Token.CLASS)) {

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
        if (token.getID() == Token.LBRACE ||
            token.getID() == Token.ELLIPSIS) {
            if (type == Token.CLASS) {
                error("template.substitution.unsupported", token);
            }

            if (token.getID() == Token.ELLIPSIS) {
                error("template.substitution.lbrace", token);
            }
            else {
                read();
                token = peek();
            }

            if (token.getID() == Token.ELLIPSIS) {
                read();
                token = peek();
                if (token.getID() == Token.RBRACE) {
                    read();
                    subParam = true;
                }
                else {
                    error("template.substitution.rbrace", token);
                }
            }
            else {
                error("template.substitution.ellipsis", token);
                if (token.getID() == Token.RBRACE) {
                    read();
                    subParam = true;
                }
            }
        }

        // Parse statements until end of file is reached.
        StatementList statementList;
        Vector<Statement> v = new Vector<Statement>(10, 0);

        SourceInfo info = peek().getSourceInfo();
        Statement statement = null;
        while (peek().getID() != Token.EOF) {
            if (peek().getID() == Token.IMPORT) {
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

        if (type == Token.TEMPLATE) {
            return new Template(templateInfo, name, params, subParam,
                            statementList, directiveList);
        }
        else if (type == Token.CLASS) {
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
        if (token.getID() != Token.IDENT) {
            if (token.isReservedWord()) {
                error("identifier.reserved.word", token.getImage(), token);
                return token.getImage();
            }
            else {
                error("identifier.expected", token);
                return "";
            }
        }

        return token.getStringValue();
    }

    private Name parseName() throws IOException {
        SourceInfo info = null;
        StringBuffer name = new StringBuffer(20);

        while (true) {
            Token token = read();
            if (token.getID() != Token.IDENT) {
                if (info == null) {
                    info = token.getSourceInfo();
                }
                else {
                    info = info.setEndPosition(token.getSourceInfo());
                }

                if (token.isReservedWord()) {
                    error("name.reserved.word", token.getImage(), token);
                    name.append(token.getImage());
                }
                else {
                    error("name.identifier.expected", token);
                    break;
                }
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
            if (token.getID() != Token.DOT) {
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
        while (peek().getID() == Token.LBRACK) {
            dim++;
            Token token = read(); // read the left bracket
            if (peek().getID() == Token.RBRACK) {
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
        if (token.getID() == Token.HASH) {
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

        if (token.getID() == Token.LPAREN) {
            read(); // read the left paren
            token = peek();
        }
        else {
            error("params.lparen", token);
        }

        Vector<Variable> vars = new Vector<Variable>(10, 0);

        if (token.getID() == Token.RPAREN) {
            // Empty list detected.
        }
        else {
            while (true) {
                if ((token = peek()).getID() == Token.RPAREN) {
                    error("params.premature.end", token);
                    break;
                }

                vars.addElement(parseVariableDeclaration(false));

                if ((token = peek()).getID() != Token.COMMA) {
                    break;
                }
                else {
                    read(); // read the comma
                }
            }
        }

        if (token.getID() == Token.RPAREN) {
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
        if (token.getID() != Token.IDENT) {
            if (token.isReservedWord()) {
                error("lvalue.reserved.word", token.getImage(), token);
                loopVarName = token.getImage();
            }
            else {
                error("lvalue.identifier.expected", token);
                loopVarName = "";
            }
        }
        else {
            loopVarName = token.getStringValue();
        }

        return new VariableRef(token.getSourceInfo(), loopVarName);
    }

    private Block parseBlock() throws IOException {
        Token token = peek();
        SourceInfo info = token.getSourceInfo();

        if (token.getID() != Token.LBRACE) {
            error("block.lbrace.expected", token);
            if (token.getID() == Token.SEMI) {
                read();
                return new Block(info, new Statement[0]);
            }
        }
        else {
            token = read(); // read the left brace
        }

        Vector<Statement> v = new Vector<Statement>(10, 0);
        Token p;
        while ((p = peek()).getID() != Token.RBRACE) {
            if (p.getID() == Token.EOF) {
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

            switch (token.getID()) {
            case Token.SEMI:
                // If the token after the semi-colon is a right brace,
                // we can't simply skip it because this method
                // can't properly parse a right brace. Instead, return
                // an empty placeholder statement. The parseBlock method
                // will then be able to parse the right brace properly.

                int ID = peek().getID();
                if (ID == Token.RBRACE || ID == Token.EOF) {
                    st = new Statement(token.getSourceInfo());
                }
                else {
                    // Skip this token
                }
                break;
            case Token.BREAK:
                st = parseBreakStatement(token);
                break;
            case Token.CONTINUE:
                st = parseContinueStatement(token);
                break;
            case Token.IF:
                st = parseIfStatement(token);
                break;
            case Token.FOREACH:
                st = parseForeachStatement(token);
                break;
            case Token.DEFINE:
                SourceInfo info = token.getSourceInfo();
                Variable v = parseVariableDeclaration(true);
                VariableRef lvalue = new VariableRef(info, v.getName());
                lvalue.setVariable(v);
                Expression rvalue = new NullLiteral(info);   // Empty expression is null assignment

                if (peek().getID() == Token.ASSIGN) {
                    read();
                    rvalue = parseExpression();
                    info = info.setEndPosition(rvalue.getSourceInfo());
                }

                st = new AssignmentStatement(info, lvalue, rvalue);
                break;
            case Token.IDENT:
                if (peek().getID() == Token.ASSIGN) {
                    st = parseAssignmentStatement(token);
                }
                else {
                    st = new ExpressionStatement(parseExpression(token));
                }
                break;

            case Token.EOF:
                error("statement.expected", token);
                st = new Statement(token.getSourceInfo());
                break;

                // Handle some error cases in a specialized way so that
                // the error message produced is more meaningful.
            case Token.ELSE:
                error("statement.misuse.else", token);
                st = parseBlock();
                break;
            case Token.IN:
                error("statement.misuse.in", token);
                st = new ExpressionStatement(parseExpression(token));
                break;

            case Token.REVERSE:
                error("statement.misuse.reverse", token);
                st = new ExpressionStatement(parseExpression(token));
                break;

            default:
                st = new ExpressionStatement(parseExpression(token));
                break;
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
        if (token.getID() != Token.ELSE) {
            info = info.setEndPosition(thenPart.getSourceInfo());
        }
        else {
            read(); // read the else keyword
            token = peek();
            if (token.getID() == Token.IF) {
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
        if (token.getID() == Token.LPAREN) {
            read();
        }
        else {
            error("foreach.lparen.expected", token);
        }

        VariableRef loopVar = parseLValue();

        // mod for declarative typing
        boolean foundASToken = false;
        Token asToken = peek();
        if (asToken.getID() == Token.AS) {
            foundASToken = true;
            read();
            TypeName typeName = parseTypeName();
            SourceInfo info2 = peek().getSourceInfo();
            loopVar.setVariable(new Variable(info2, loopVar.getName(), typeName, true));
        }
        // end mod

        token = peek();
        if (token.getID() == Token.IN) {
            read();
        }
        else {
            error("foreach.in.expected", token);
        }

        Expression range = parseExpression();
        Expression endRange = null;

        token = peek();
        if (token.getID() == Token.DOTDOT) {
            read();
            endRange = parseExpression();
            token = peek();
        }

        if (endRange != null && foundASToken)
            error("foreach.as.not.allowed", asToken);

        boolean reverse = false;
        if (token.getID() == Token.REVERSE) {
            read();
            reverse = true;
            token = peek();
        }

        if (token.getID() == Token.RPAREN) {
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

        if (peek().getID() == Token.ASSIGN) {
            read();
        }
        else {
            error("assignment.equals.expected", peek());
        }

        Expression rvalue = parseExpression();

        info = info.setEndPosition(rvalue.getSourceInfo());

        // Start mod for 'as' keyword for declarative typing
        if (peek().getID() == Token.AS) {
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

        Token token = peek();
        SourceInfo info = token.getSourceInfo();

        if (token.getID() == leftID) {
            read(); // read the left paren
            token = peek();
        }
        else {
            if (lbracket == Token.LPAREN) {
                error("list.lparen.expected", token);
            }
            else if (lbracket == Token.LBRACK) {
                error("list.lbracket.expected", token);
            }
            else if (lbracket == Token.LBRACE) {
                error("list.lbrace.expected", token);
            }
            else {
                error("list.expected", token);
            }
        }

        Vector<Expression> exprs = new Vector<Expression>(10, 0);
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

                if (token.getID() != Token.COMMA &&
                    token.getID() != Token.EQUAL_GREATER &&
                    token.getID() != Token.COLON) {
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
                if (rbracket == Token.RPAREN) {
                    error("list.rparen.expected", token);
                }
                else if (rbracket == Token.RBRACK) {
                    error("list.rbracket.expected", token);
                }
                else if (rbracket == Token.RBRACE) {
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
        if (token.getID() == Token.QUESTION) {
            read();
            token = peek();
            if (token.getID() == Token.COLON) {
                read();

                Expression thenExpr = expr;
                Expression elseExpr = parseTernaryExpression(read());
                info = info.setEndPosition(elseExpr.getSourceInfo());
                expr = new TernaryExpression(info, expr, thenExpr, elseExpr);
            }
            else {
                Expression thenExpr = parseTernaryExpression(read());

                token = peek();
                if (token.getID() != Token.COLON) {
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

            if (token.getID() == Token.OR) {
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

            if (token.getID() == Token.AND) {
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

            switch (token.getID()) {
            case Token.ASSIGN:
                error("equality.misuse.assign", token);
                token = new Token(token.getSourceInfo(), Token.EQ);
            case Token.EQ:
            case Token.NE:
                read();
                Expression right = parseRelationalExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, right);
                break;
            default:
                break loop;
            }
        }

        return expr;
    }

    // TODO: spaceship/comparison expression
    /*
    parseCompareExpression(token)
        parseLeft
        read(SPACESHIP)
        parseRight
        new CompareExpression()
        
    typeCheck
        setTYpe(int)
        if (left is int, float, long, short, byte, double, numeric)
            if (right is also)
                strait compare
            else if (right is object compatible (Number))
                if object nullable
                    convert left to Number // TODO: or just handle NPE case?
                    compareTo
                else if non-null
                    convert right to int
                    strait compare
            else
                error incompatible types
         if left is boolean
            if (right is also)
                equal compare only (-1 else)
            else if (right is Boolean)
                do conversions
                compareTo
            else
                error incompatible type
          if left is non-primitive
              if right is primitive
                  do above switching left and right
              else if types compatible and both implement comparable
                  compareTo
              else // TODO: or do we invoke toString().compareTo(...)
                  error incompatible types
    
    optimizer
    
    
    generator
        invoke compareTo, ifeq, iflt, etc and create branches (store 0, 1, -1)
        if method
            generate left
            generate right
            invokevirtual compareTo
        else
            generate left
            dup
            generate right
            swap
            dup
            L0
                ifneq L1
                pop
                pop
                iconst 0
                goto L3
            L1
                iflt L2
                iconst 1
                goto L3
            L2
                iconst -1
                goto L3
            L3
   
    */
    
    private Expression parseRelationalExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();
        Expression expr = parseCompareExpression(token);

    loop:
        while (true) {
            token = peek();

            switch (token.getID()) {
            case Token.LT:
            case Token.GT:
            case Token.LE:
            case Token.GE:
                read();
                Expression right = parseCompareExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, right);
                break;
            case Token.ISA:
                read();
                TypeName typeName = parseTypeName();
                info = info.setEndPosition(typeName.getSourceInfo());
                expr = new RelationalExpression(info, token, expr, typeName);
                break;
            default:
                break loop;
            }
        }

        return expr;
    }

    private Expression parseCompareExpression(Token token)
        throws IOException {
        
        SourceInfo info = token.getSourceInfo();
        Expression expr = parseConcatenateExpression(token);

    loop:
        while (true) {
            token = peek();

            if (token.getID() == Token.SPACESHIP) {
                read();
                Expression right = parseConcatenateExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new CompareExpression(info, token, expr, right);
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

            if (token.getID() == Token.CONCAT) {
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

            switch (token.getID()) {
            case Token.PLUS:
            case Token.MINUS:
                read();
                Expression right = parseMultiplicativeExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new ArithmeticExpression(info, token, expr, right);
                break;
            default:
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

            switch (token.getID()) {
            case Token.MULT:
            case Token.DIV:
            case Token.MOD:
                read();
                Expression right = parseUnaryExpression(read());
                info = info.setEndPosition(right.getSourceInfo());
                expr = new ArithmeticExpression(info, token, expr, right);
                break;
            default:
                break loop;
            }
        }

        return expr;
    }

    private Expression parseUnaryExpression(Token token) throws IOException {
        SourceInfo info;
        Expression expr;

        switch (token.getID()) {
        case Token.NOT:
            info = token.getSourceInfo();
            expr = parseUnaryExpression(read());
            info = info.setEndPosition(expr.getSourceInfo());
            return new NotExpression(info, expr);
        case Token.MINUS:
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
            boolean nullsafe = false;

            // check for null-safe or spread operators
            Token prefix = null;
            if (token.getID() == Token.QUESTION) {
                
                prefix = read();
                token = peek();
                if (token.getID() == Token.DOT || 
                    token.getID() == Token.LBRACK) {
                    
                    nullsafe = true;
                }
                else {
                    unread(prefix);
                }
            }

            if (!nullsafe && token.getID() == Token.SPREAD) {
                // "spread" lookup i.e.: a*.b
                
                Token spread = read(); // read the spread operator
                
                token = read();

                Name lookupName;
                SourceInfo nameInfo = token.getSourceInfo();
                if (token.getID() != Token.IDENT) {
                    if (token.isReservedWord()) {
                        error("lookup.reserved.word", token.getImage(), token);
                        lookupName = new Name(nameInfo, token.getImage());
                    }
                    else {
                        error("lookup.identifier.expected", token);
                        lookupName = new Name(nameInfo, null);
                    }
                }
                else {
                    lookupName = new Name(nameInfo, token.getStringValue());
                    info = info.setEndPosition(nameInfo);
                }

                // check if function call rather than purely lookup
                Expression operation = null;
                Expression noop = new NoOpExpression(info);
                if (peek().getID() == Token.LPAREN) {
                    operation = parseCallExpression
                    (
                        FunctionCallExpression.class, noop, lookupName, info
                    );
                }
                else {
                    operation = new Lookup(info, noop, spread, lookupName);
                }
                
                // create spread expression
                expr = new SpreadExpression(info, expr, operation);
            }
            else if (token.getID() == Token.DOT) {
                // "dot" lookup i.e.: a.b

                Token dot = read(); // read the dot

                token = read();

                Name lookupName;
                SourceInfo nameInfo = token.getSourceInfo();
                if (token.getID() != Token.IDENT) {
                    if (token.isReservedWord()) {
                        error("lookup.reserved.word", token.getImage(), token);
                        lookupName = new Name(nameInfo, token.getImage());
                    }
                    else {
                        error("lookup.identifier.expected", token);
                        lookupName = new Name(nameInfo, null);
                    }
                }
                else {
                    lookupName = new Name(nameInfo, token.getStringValue());
                    info = info.setEndPosition(nameInfo);
                }

                // check if function call rather than purely lookup
                if (peek().getID() == Token.LPAREN) {
                    expr = parseCallExpression(FunctionCallExpression.class,
                                               expr, lookupName, info);
                }
                else {
                    expr = new Lookup(info, expr, dot, lookupName);
                }
            }
            else if (token.getID() == Token.LBRACK) {
                // array lookup i.e.: a[b]

                Token lbrack = read(); // read the left bracket

                token = read();

                if (token.getID() == Token.RBRACK) {
                    info = info.setEndPosition(token.getSourceInfo());

                    error("lookup.empty.brackets", token);

                    expr = new ArrayLookup(info, expr, lbrack,
                                           new Expression(info));

                    continue;
                }

                Expression arrayLookup = parseExpression(token);

                token = peek();

                if (token.getID() == Token.RBRACK) {
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
            
            if (expr instanceof NullSafe) {
                ((NullSafe) expr).setNullSafe(nullsafe);
            }
        }

        return expr;
    }

    private Expression parseFactor(Token token) throws IOException {
        SourceInfo info = token.getSourceInfo();

        switch (token.getID()) {
        case Token.HASH:
        case Token.DOUBLE_HASH:
            return parseNewArrayExpression(token);

        case Token.LBRACE:
            Token next = peek();
            Expression lambda;
            if (next.getID() == Token.RBRACE) {
                lambda = null;
            }
            else {
                lambda = parseLambda(token);
            }

            if (lambda == null) {
                error("factor.empty.braces", info);
                lambda = new Expression(info);
            }

            return lambda;

        case Token.LPAREN:
            Expression expr;

            token = peek();
            if (token.getID() == Token.RPAREN) {
                expr = null;
            }
            else {
                expr = parseExpression(read());
            }

            token = peek();
            if (token.getID() == Token.RPAREN) {
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

        case Token.NULL:
            return new NullLiteral(info);

        case Token.TRUE:
            return new BooleanLiteral(info, true);

        case Token.FALSE:
            return new BooleanLiteral(info, false);

        case Token.CALL:
            return parseTemplateCallExpression(token);
            
        case Token.ELLIPSIS:
            return parseSubstitutionExpression(token);

        case Token.NUMBER:
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

        case Token.STRING:
            return new StringLiteral(info, token.getStringValue());

        case Token.IDENT:
            FunctionCallExpression call = parseFunctionCallExpression(token);
            if (call != null) {
                return call;
            }
            else {
                return new VariableRef(info, token.getStringValue());
            }

        case Token.EOF:
            error("factor.expression.expected", token);
            break;

        case Token.RPAREN:
            error("factor.rparen.unmatched", token);
            break;

        case Token.RBRACE:
            error("factor.rbrace.unmatched", token);
            break;

        case Token.RBRACK:
            error("factor.rbracket.unmatched", token);
            break;

        case Token.ASSIGN:
            error("factor.illegal.assignment", token);
            break;

        case Token.DOTDOT:
            error("factor.misuse.dotdot", token);
            break;

        default:
            if (token.isReservedWord()) {
                error("factor.reserved.word", token.getImage(), token);
            }
            else {
                error("factor.unexpected.token", token);
            }
            break;
        }

        return new Expression(token.getSourceInfo());
    }

    private Expression parseNewArrayExpression(Token token)
        throws IOException {

        Name name = null;
        Token next = peek();
        if (next.getID() == Token.IDENT) {
            name = parseName();
        }

        boolean anonymous = (name == null && next.getID() == Token.LBRACE);
        boolean associative = (token.getID() == Token.DOUBLE_HASH);

        SourceInfo info = token.getSourceInfo();
        ExpressionList list = null;
        if (name != null || anonymous) {
            list = parseList(Token.LBRACE, Token.RBRACE, associative);
        } else {
            list = parseList(Token.LPAREN, Token.RPAREN, associative);
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

    private LambdaExpression parseLambda(Token brace)
        throws IOException {
        
        // Check if a block is being passed in the call.
        LambdaStatement lambda = null;
        Vector<Token> lookahead = new Vector<Token>();
        
        // retrieve opening brace
        SourceInfo info = brace.getSourceInfo();
        
        // search for a lambda statement: ident,ident ->
        boolean found = false;
        
        while (true) {
            Token t = read();
            lookahead.add(t);
            if (t.getID() == Token.IDENT || t.getID() == Token.COMMA) {
                continue;
            }
            else if (t.getID() == Token.LAMBDA) {
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
            lambda = parseLambdaStatement();
        }

        // unread brace to parse block
        unread(brace);

        // process block
        Block block = parseBlock();
        info = info.setEndPosition(block.getSourceInfo());

        // update statement
        if (lambda != null) {
            lambda.setBlock(block);
            block = new LambdaBlock(lambda);
        }
        
        // ensure lambda block
        LambdaBlock lblock = null;
        if (block instanceof LambdaBlock) {
            lblock = (LambdaBlock) block;
        }
        else {
            lblock = new LambdaBlock(block);
        }
    
        // return expr
        return new LambdaExpression(info, lblock);
    }
    
    private LambdaStatement parseLambdaStatement()
        throws IOException {
        SourceInfo info = null;
        List<VariableRef> vars = new ArrayList<VariableRef>();
        while (true) {
            // read either name or type name
            Token token1 = read();
            if (token1.getID() != Token.IDENT) {
                error("function.missing.identifer", token1);

                if (token1.getID() == Token.LAMBDA) { break; }
                else { continue; }
            }

            // save info
            if (info == null) { info = token1.getSourceInfo(); }
            else { info = info.setEndPosition(token1.getSourceInfo()); }
            
            // read next token as either name or next declaration and then
            // unread first token to allow parsing name/type
            Token token2 = peek();
            unread(token1);
            
            // if both idents, lookup type and name
            VariableRef ref = null;
            if (token2.getID() == Token.IDENT) {
                // re-read during parsing
                Variable variable = parseVariableDeclaration(false);
                ref = new VariableRef(variable.getSourceInfo(), variable.getName());
                ref.setVariable(variable);
                
                // update info
                info = info.setEndPosition(token2.getSourceInfo());
            }
            
            // otherwise, just use name w/ default type
            else {
                Name name = parseName();
                ref = new VariableRef(name.getSourceInfo(), name.getName());
            }
            
            // add variable ref
            vars.add(ref);

            // process next token
            Token next = read();
            if (next.getID() == Token.COMMA) {
                continue;
            }
            else if (next.getID() == Token.LAMBDA) {
                break;
            }
        }

        return new LambdaStatement(info,
                                   vars.toArray(new VariableRef[vars.size()]),
                                   null);
    }

    private TemplateCallExpression parseTemplateCallExpression(Token token)
        throws IOException {
        
        SourceInfo info = token.getSourceInfo();
        
        Name target = parseName();
        info.setEndPosition(target.getSourceInfo());

        // parse remainder of call expression
        return parseCallExpression(TemplateCallExpression.class, 
                                   null, target, info);
    }
    
    // Special parse method in that it may return null if it couldn't parse
    // a FunctionCallExpression. Token passed in must be an identifier.
    private FunctionCallExpression parseFunctionCallExpression(Token token)
        throws IOException {

        SourceInfo info = token.getSourceInfo();

        // Search for pattern <ident> {<dot> <ident>} <lparen>
        Vector<Token> lookahead = new Vector<Token>();
        StringBuffer name = new StringBuffer(token.getStringValue());
        Name target = null;

        while (true) {
            token = read();
            lookahead.addElement(token);

            if (token.getID() == Token.DOT) {
                name.append('.');
                info = info.setEndPosition(token.getSourceInfo());
            }
            else if (token.getID() == Token.LPAREN) {
                target = new Name(info, name.toString());
                unread(token);
                break;
            }
            else {
                break;
            }

            token = read();
            lookahead.addElement(token);

            if (token.getID() == Token.IDENT) {
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
        
        // parse remainder of call expression
        return parseCallExpression(FunctionCallExpression.class, 
                                   null, target, info);
    }

    private <T extends CallExpression> 
    T parseCallExpression(Class<T> clazz, Expression expression, Node target, 
                          SourceInfo info)
        throws IOException {
        
        ExpressionList list =
            parseList(Token.LPAREN, Token.RPAREN, false);
        info = info.setEndPosition(list.getSourceInfo());

        // Check if a block is being passed in the call.
        Expression subParam = null;
        if (peek().getID() == Token.LBRACE) {
            subParam = parseLambda(read());
        }
        
        // lookup ctor and invoke
        try {
            return clazz.getConstructor(SourceInfo.class, Expression.class,
                                        Name.class, ExpressionList.class, 
                                        LambdaExpression.class)
                        .newInstance(info, expression, target, list, subParam);
        }
        catch (Exception exception) {
            throw new IOException("unable to create ctor", exception);
        }
    }

    private SubstitutionExpression parseSubstitutionExpression(Token token)
        throws IOException {
        
        // get current source
        SourceInfo info = token.getSourceInfo();
        
        // check if defining params to array
        Token next = peek();
        ExpressionList params = null;
        if (next.getID() == Token.LPAREN) {
            params = parseList(Token.LPAREN, Token.RPAREN, false);
            info = info.setEndPosition(params.getSourceInfo());
        }
        
        // return statement
        return new SubstitutionExpression(info, params);
    }

    /** Test program */
    public static void main(String[] arg) throws Exception {
        Tester.test(arg);
    }

    /**
     *
     * @author Brian S O'Neill
     */
    private static class Tester implements ErrorListener {
        public static void test(String[] arg) throws Exception {
            new Tester(arg[0]);
        }

        public Tester(String filename) throws Exception {
            Reader file = new BufferedReader(new FileReader(filename));
            Scanner scanner = new Scanner(new SourceReader(file, "<%", "%>"));
            scanner.addErrorListener(this);
            Parser parser = new Parser(scanner);
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
