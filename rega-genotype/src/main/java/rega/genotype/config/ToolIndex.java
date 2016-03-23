package rega.genotype.config;

/**
 * Json unique tool index stored on Repo server.
 * The ToolRepoService creates ToolIndex per tool (not version)
 * @author michael
 */
public class ToolIndex {
	// publisher unique pwd created when automatically with global config and stored also global config.
	// sent by header in PUBLISH_REQ
	private String publisherPassword; 
	private String configurations; // ziped tools data on repo server (pointer to the tool).
	
	public ToolIndex(){}

	public ToolIndex(String publisherPassword, String configurations){
		this.publisherPassword = publisherPassword;
		this.configurations = configurations;
	}
	
	public String getConfigurations() {
		return configurations;
	}
	public void setConfigurations(String configurations) {
		this.configurations = configurations;
	}
	public String getPublisherPassword() {
		return publisherPassword;
	}
	public void setPublisherPassword(String publisherPassword) {
		this.publisherPassword = publisherPassword;
	}
}
