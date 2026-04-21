package labs.franklee.celero.logic.path;

import java.util.ArrayList;
import java.util.List;

public class PathGroup {
    private final List<Path> paths = new ArrayList<>();

    public PathGroup() {}

    public PathGroup(Path path) {
        this.paths.add(path);
    }

    public List<Path> paths() {
        return this.paths;
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public void sort() {
        if (!this.paths.isEmpty()) {
            this.paths.forEach(Path::sort);
        }
    }

}
