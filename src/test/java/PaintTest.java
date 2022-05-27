import com.mnzn.utils.visual.DrawableTreeNode;
import com.mnzn.utils.visual.paint.PaintUnits;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PaintTest {
    private static class TestNode implements DrawableTreeNode {
        private static int idCnt = 0;
        private final String id;
        private final List<String> values;
        private final List<DrawableTreeNode.ForwardEdge> children = new LinkedList<>();

        public TestNode(String... values) {
            this.id = String.valueOf(idCnt++);
            this.values = Arrays.stream(values).toList();
        }

        public void addChild(TestNode child, String edge) {
            children.add(new DrawableTreeNode.ForwardEdge(child, edge));
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<String> getLabels() {
            return values;
        }

        @Override
        public ForwardEdge[] getEdges() {
            return children.toArray(ForwardEdge[]::new);
        }

        public static TestNode randMake(int depth) {
            TestNode root = new TestNode(randStr());
            if (depth > 0) {
                for (int i = (int) (Math.random() * 3 + 1); i > 0; i--) {
                    TestNode child = randMake(depth - 1);
                    root.addChild(child, randStr());
                }
            }
            return root;
        }

        private static String randStr() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                sb.append((char) (Math.random() * 26 + 'a'));
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        TestNode node = TestNode.randMake(4);
        PaintUnits.paintTree(node, "./.cache/test.png");
    }
}
