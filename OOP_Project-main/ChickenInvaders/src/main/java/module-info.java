module project.chickeninvaders {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens project.chickeninvaders to javafx.fxml;
    exports project.chickeninvaders;
    exports project.chickeninvaders.entities;
    opens project.chickeninvaders.entities to javafx.fxml;
}