package labs.franklee.celero.logic.base;

import labs.franklee.celero.logic.path.PathGroup;

public abstract class Relation extends Node implements Negatable<Relation> {

    protected PathGroup pathGroup;

    public abstract RelationType relation();

    public Relation() {
        this.type = NodeType.Relation;
    }

    public void setPathGroup(PathGroup pathGroup) {
        this.pathGroup = pathGroup;
    }

    public PathGroup getPathGroup() {
        return this.pathGroup;
    }
}
