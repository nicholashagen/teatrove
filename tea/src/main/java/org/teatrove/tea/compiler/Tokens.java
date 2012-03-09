package org.teatrove.tea.compiler;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.teatrove.tea.annotations.TokenCallback;
import org.teatrove.trove.io.SourceReader;

public class Tokens {

    /** Token ID for any token. */
    public final static int ANY = -1;

    /** Token ID for an unknown token. */
    public final static int UNKNOWN = 0;

    /** Token ID for the end of file. */
    public final static int EOF = 1;

    /** Token ID for a single-line or multi-line comment. */
    public final static int COMMENT = 2;

    /** Token ID for the start of a code region. */
    public final static int ENTER_CODE = 3;

    /** Token ID for the start of a text region. */
    public final static int ENTER_TEXT = 4;

    /** Token ID for a string literal. */
    public final static int STRING = 5;

    /** Token ID for a number literal. */
    public final static int NUMBER = 6;

    /** Token ID for an identifier. */
    public final static int IDENT = 7;

    /** Last preset token. */
    public final static int LAST_TOKEN = 7;

    /** State of whether tokens have been loaded. */
    private boolean mLoaded;

    /** Annotation database and indexing. */
    private Annotations mAnnotations;

    /** The next available id for dynamically loaded tokens. */
    private int mNextId = LAST_TOKEN + 1;

    /** List of token definitions (sorted accordingly). */
    private Set<TokenDefinition> mTokens =
        new TreeSet<TokenDefinition>(Collections.reverseOrder());

    /** List of keyword definitions (sorted accordingly). */
    private Set<KeywordDefinition> mKeywords =
        new TreeSet<KeywordDefinition>(Collections.reverseOrder());

    /**
     * Default constructor.
     */
    public Tokens(Annotations annotations) {
        mAnnotations = annotations;
    }

    /**
     * Find the associated token definition based on the specified token image.
     *
     * @param token  the token image
     *
     * @return  the associated definition or <code>null</code>
     */
    public TokenDefinition findToken(String token) {
        // load if necessary
        load();

        // search for matching token
        for (TokenDefinition definition : mTokens) {
            if (token.equals(definition.getToken())) {
                return definition;
            }
        }

        // none found
        return null;
    }

    /**
     * Find the associated keyword definition based on the specified keyword.
     *
     * @param keyword  the keyword
     *
     * @return  the associated definition or <code>null</code>
     */
    public KeywordDefinition findKeyword(String keyword) {
        // load if necessary
        load();

        // search for matching keyword
        for (KeywordDefinition definition : mKeywords) {
            if (keyword.equals(definition.getKeyword())) {
                return definition;
            }
        }

        // none found
        return null;
    }

    public Token makeToken(int id, String name, SourceReader source) {
        load();
        return makeToken(id, name, source.getLineNumber(),
                         source.getStartPosition(), source.getEndPosition());
    }

    public Token makeToken(int id, String name, String str,
                           SourceReader source) {
        load();
        return makeToken(id, name, str, source.getLineNumber(),
                         source.getStartPosition(), source.getEndPosition());
    }

    public Token makeToken(int id, String name,
                           int lineNumber, int startPos, int endPos) {
        load();
        return new Token(lineNumber, startPos, endPos, id, name);
    }

    public Token makeToken(int id, String name, String str,
                           int lineNumber, int startPos, int endPos) {
        load();
        return new StringToken(lineNumber, startPos, endPos,
                               id, name, str);
    }

    public Token makeToken(int id, String name, String str,
                           int lineNumber, int startPos, int endPos,
                           int errorPos) {
        load();
        return new StringToken(lineNumber, startPos, endPos, errorPos,
                               id, name, str);
    }

    public Token scanToken(int c, SourceReader source)
        throws IOException {

        // load
        load();

        // mark info
        int line = source.getLineNumber();
        int startPos = source.getStartPosition();

        // search each registered token
        outer: for (TokenDefinition definition : mTokens) {
            // process callback if available
            TokenCallback callback = definition.getCallback();
            if (callback != null) {
                try {
                    Token token = callback.scanToken(source);
                    if (token != null) { return token; }
                }
                catch (Exception e) {
                    throw new IOException("unable to invoke callback", e);
                }
            }

            // process token
            else {
                String token = definition.getToken();
                if (token.charAt(0) != c) { continue; }

                for (int i = 1; i < token.length(); i++) {
                    int ch = token.charAt(i);
                    if (source.read() != ch) {
                        source.unread(i);
                        continue outer;
                    }
                }

                // define start
                SourceInfo sourceInfo =
                    new SourceInfo(line, startPos, source.getEndPosition());

                // return token instance
                return new Token(sourceInfo, definition.getId(),
                                     definition.getName());
            }
        }

        // none found
        return null;
    }

    protected void load() {
        // ignore if already loaded
        if (mLoaded) { return; }

        // load annotations processing errors
        mLoaded = true;
        try { loadAnnotations(); }
        catch (Exception e) {
            throw new IllegalArgumentException("unable to load tokens", e);
        }
    }

    protected void loadAnnotations()
        throws Exception {

        // load tokens
        Class<?>[] tokenClasses =
            mAnnotations.getAnnotations(org.teatrove.tea.annotations.Token.class);
        for (Class<?> clazz : tokenClasses) {
            loadToken(clazz);
        }

        // load keywords
        Class<?>[] keywordClasses =
            mAnnotations.getAnnotations(org.teatrove.tea.annotations.Keyword.class);
        for (Class<?> clazz : keywordClasses) {
            loadKeyword(clazz);
        }
    }

    protected void loadToken(Class<?> clazz) throws Exception {
        // search for token declared fields
        outer: for (Field field : clazz.getDeclaredFields()) {

            // get token and ignore if not specified
            org.teatrove.tea.annotations.Token token =
                field.getAnnotation(org.teatrove.tea.annotations.Token.class);
            if (token == null) { continue; }

            // verify annotation
            if (token.callback() == null &&
                (token.value() == null || token.value().isEmpty())) {
                throw new TokenDefinitionException
                (
                    token, field,
                    "token must have a valid token value or callback"
                );
            }

            // verify format
            field.setAccessible(true);
            if (!field.getType().equals(int.class)) {
                throw new TokenDefinitionException
                (
                    token, field, "field must be declared as type int"
                );
            }

            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                throw new TokenDefinitionException
                (
                    token, field, "field must be declared as static"
                );
            }

            if (!TokenCallback.class.equals(token.callback())) {
                if (token.callback().isInterface()) {
                    throw new TokenDefinitionException
                    (
                        token, field, "token must have valid callback class"
                    );
                }

                if (token.callback().getConstructor() != null) {
                    throw new TokenDefinitionException
                    (
                        token, field, "token must have valid callback ctor"
                    );
                }
            }

            // verify if previously declared
            TokenDefinition previous = null;
            if (token.value() != null && !token.value().isEmpty()) {
                previous = findToken(token.value());
            }

            if (previous != null &&
                previous.getToken().equals(token.value())) {

                // verify matching priority
                if (previous.getPriority() != token.priority()) {
                    throw new TokenDefinitionException
                    (
                        token, field,
                        "multiple incompatible definitions for token " +
                        "(expected priority of " + previous.getPriority() + ")"
                    );
                }

                // update field with expected value (no need to add)
                field.set(null, Integer.valueOf(previous.getId()));
                continue outer;
            }

            // create definition
            int id = mNextId++;
            if (token.value() != null && !token.value().isEmpty()) {
                mTokens.add
                (
                    new TokenDefinition(id, field.getName(),
                                        token.value(), token.priority())
                );
            }
            else {
                mTokens.add
                (
                    new TokenDefinition(id, field.getName(),
                                        token.callback(), token.priority())
                );
            }

            // update field
            field.set(null, Integer.valueOf(id));
        }
    }

    protected void loadKeyword(Class<?> clazz) throws Exception {
        // search for keyword declared fields
        outer: for (Field field : clazz.getDeclaredFields()) {

            // get keyword and ignore if not specified
            org.teatrove.tea.annotations.Keyword keyword =
                field.getAnnotation(org.teatrove.tea.annotations.Keyword.class);
            if (keyword == null) { continue; }

            // verify annotation
            if (keyword.value() == null || keyword.value().isEmpty()) {
                throw new KeywordDefinitionException
                (
                    keyword, field, "keyword must have a valid keyword value"
                );
            }

            // verify format
            field.setAccessible(true);
            if (!field.getType().equals(int.class)) {
                throw new KeywordDefinitionException
                (
                    keyword, field, "field must be declared as type int"
                );
            }

            if ((field.getModifiers() & Modifier.STATIC) == 0) {
                throw new KeywordDefinitionException
                (
                    keyword, field, "field must be declared as static"
                );
            }

            // verify if previously declared
            KeywordDefinition previous = findKeyword(keyword.value());
            if (previous != null &&
                previous.getKeyword().equals(keyword.value())) {

                // verify matching priority
                if (previous.getPriority() != keyword.priority()) {
                    throw new KeywordDefinitionException
                    (
                        keyword, field,
                        "multiple incompatible definitions for token " +
                        "(expected priority of " + previous.getPriority() + ")"
                    );
                }

                // update field with expected value (no need to add)
                field.set(null, Integer.valueOf(previous.getId()));
                continue outer;
            }

            // create definition
            int id = mNextId++;
            mKeywords.add
            (
                new KeywordDefinition(id, field.getName(),
                                      keyword.value(), keyword.priority())
            );

            // update field
            field.set(null, Integer.valueOf(id));
        }
    }
}
