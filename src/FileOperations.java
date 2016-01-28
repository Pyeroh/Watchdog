
public enum FileOperations {

	COPY("Copier"),
	MOVE("Déplacer"),
	DELETE("Supprimer");
	
	private final String label;
	
	private FileOperations(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}

}
