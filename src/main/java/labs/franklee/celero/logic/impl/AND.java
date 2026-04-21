package labs.franklee.celero.logic.impl;

import labs.franklee.celero.logic.base.*;
import labs.franklee.celero.logic.path.Path;
import labs.franklee.celero.logic.path.PathGroup;

import java.util.ArrayList;
import java.util.List;

public class AND extends Relation {

    public AND() {
        super();
    }

    public AND(Path path) {
        super();
        this.setPathGroup(new PathGroup(path));
    }

    @Override
    public Validation validate() {
        boolean b = !this.getChildren().isEmpty();
        return b ? new Validation(true, null) :
                new Validation(false, "And node mush have at least one node.");
    }

    @Override
    public RelationType relation() {
        return RelationType.And;
    }

    @Override
    public Relation resolve() throws Exception {
        if (this.getChildren().size() == 1) {
            Node node = this.getChildren().getFirst();
            return node.resolve();
        }
        Relation currentNode = null;
        for (int i = 1; i < this.getChildren().size(); i++) {
            if (currentNode == null) {
                Node n0 = this.getChildren().getFirst();
                currentNode = n0.resolve();
            }
            Node next = this.getChildren().get(i);
            Relation nextNode = next.resolve();
            PathGroup pathGroup = new PathGroup();
            Relation newCurrent;
            if (currentNode.relation() == RelationType.And) {
                Path p1 = currentNode.getPathGroup().paths().getFirst();
                if (nextNode.relation() == RelationType.And) {
                    Path p2 = nextNode.getPathGroup().paths().getFirst();
                    Path path = new Path();
                    path.addAllCondition(p1.conditions());
                    path.addAllCondition(p2.conditions());
                    pathGroup.addPath(path);
                    newCurrent = new AND();
                    newCurrent.setPathGroup(pathGroup);
                } else {
                    for (Path p2 : nextNode.getPathGroup().paths()) {
                        Path path = new Path();
                        path.addAllCondition(p1.conditions());
                        path.addAllCondition(p2.conditions());
                        pathGroup.addPath(path);
                    }
                    newCurrent = new OR();
                    newCurrent.setPathGroup(pathGroup);
                }
            } else {
                List<Path> paths = currentNode.getPathGroup().paths();
                if (nextNode.relation() == RelationType.And) {
                    Path p2 = nextNode.getPathGroup().paths().getFirst();
                    for (Path p : paths) {
                        Path path = new Path();
                        path.addAllCondition(p.conditions());
                        path.addAllCondition(p2.conditions());
                        pathGroup.addPath(path);
                    }
                    newCurrent = new OR();
                    newCurrent.setPathGroup(pathGroup);
                } else {
                    List<Path> ps = nextNode.getPathGroup().paths();
                    for (Path p : paths) {
                        for (Path p2 : ps) {
                            Path path = new Path();
                            path.addAllCondition(p.conditions());
                            path.addAllCondition(p2.conditions());
                            pathGroup.addPath(path);
                        }
                    }
                    newCurrent = new OR();
                    newCurrent.setPathGroup(pathGroup);
                }
            }
            currentNode = newCurrent;
        }
        return currentNode;
    }

    @Override
    public Relation negate() throws Exception {
        Relation or = new OR();
        List<Node> nodes = new ArrayList<>(this.getChildren().size());
        for (Node child : this.getChildren()) {
            if (child instanceof Negatable) {
                nodes.add(((Negatable<?>) child).negate());
            } else {
                throw new UnsupportedOperationException("Negatable child node is required. " + child.getClass().getName() + " is not negatable!");
            }
        }
        or.setChildren(nodes);
        return or;
    }
}
