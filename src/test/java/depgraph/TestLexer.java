package depgraph;

import depgraph.Parser.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TestLexer {
    private Lexer lexer = new Lexer();

    @Test
    public void testTokenizerReturnsIgnoredForUnkownStatements() {
        Token token = lexer.tokenize("unknownsymbol");
        assertEquals(TokenTypeEnum.IGNORED, token.getToken());
    }

    @Test
    public void testTokenizerReturnsNodeAttrStmtForNodeStatements() {
        Token token = lexer.tokenize("node [somestuffwedontcareabout];");
        assertEquals(TokenTypeEnum.NODE_ATTR_STMT, token.getToken());
    }

    @Test
    public void testTokenizerReturnsEdgeAttrStmtForEdgeStatements() {
        Token token = lexer.tokenize("edge [somestuffwedontcareabout]");
        assertEquals(TokenTypeEnum.EDGE_ATTR_STMT, token.getToken());
    }

    @Test
    public void testTokenizerReturnsNodeStmtForNodeDefinitions() {
        Token token = lexer.tokenize("Node1 [attributes];");
        assertEquals(TokenTypeEnum.NODE_STMT, token.getToken());
    }

    @Test
    public void testTokenizerReturnsNodeStmtForNodeDefinitionsWithoutAttributes() {
        Token token = lexer.tokenize("Node1;");
        assertEquals(TokenTypeEnum.NODE_STMT, token.getToken());
    }

    @Test
    public void testTokenizerReturnsEdgeStmtForEdgeDefinitions() {
        Token token = lexer.tokenize("Node1 -> Node2 [attributes];");
        assertEquals(TokenTypeEnum.EDGE_STMT, token.getToken());
    }

    @Test
    public void testTokenizerReturnsEdgeStmtForEdgeDefinitionsWithoutAttributes() {
        Token token = lexer.tokenize("Node1 -> Node2;");
        assertEquals(TokenTypeEnum.EDGE_STMT, token.getToken());
    }
}
