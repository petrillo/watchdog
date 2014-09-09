package nl.tudelft.watchdog.ui.wizards.projectregistration;

import nl.tudelft.watchdog.logic.exceptions.ServerCommunicationException;
import nl.tudelft.watchdog.logic.logging.WatchDogLogger;
import nl.tudelft.watchdog.logic.network.JsonTransferer;
import nl.tudelft.watchdog.ui.wizards.Project;
import nl.tudelft.watchdog.ui.wizards.RegistrationEndingPage;

/**
 * Possible finishing page in the wizard. If the project exists on the server,
 * or the server is not reachable, the user can exit here.
 */
class ProjectCreatedEndingPage extends RegistrationEndingPage {

	@Override
	protected void makeRegistration() {
		Project project = new Project();

		ProjectSliderPage sliderPage;
		ProjectRegistrationPage projectPage = null;
		if (getPreviousPage() instanceof ProjectRegistrationPage) {
			projectPage = (ProjectRegistrationPage) getPreviousPage();
		} else if (getPreviousPage() instanceof ProjectSliderPage) {
			sliderPage = (ProjectSliderPage) getPreviousPage();
			projectPage = (ProjectRegistrationPage) getPreviousPage()
					.getPreviousPage();

			project.productionPercentage = sliderPage.percentageProductionSlider
					.getSelection();
			project.useJunitOnlyForUnitTesting = sliderPage
					.usesJunitForUnitTestingOnly();
			project.followTestDrivenDesign = sliderPage.usesTestDrivenDesing();
		}

		// initialize from projectPage
		project.belongToASingleSofware = !projectPage.noSingleProjectButton
				.getSelection();
		project.name = projectPage.projectNameInput.getText();
		project.website = projectPage.projectWebsite.getText();
		project.usesJunit = projectPage.usesJunit();
		project.usesOtherTestingFrameworks = projectPage
				.usesOtherTestingFrameworks();
		project.usesOtherTestingForms = projectPage.usesOtherTestingForms();

		windowTitle = "Project Registration";

		try {
			id = new JsonTransferer().registerNewProject(project);
		} catch (ServerCommunicationException exception) {
			successfulRegistration = false;
			messageTitle = "Problem creating new project!";
			messageBody = exception.getMessage();
			messageBody += "\nAre you connected to the internet, and is port 80 open?";
			messageBody += "\nPlease contact us via www.testroots.org. \nWe'll troubleshoot the issue!";
			WatchDogLogger.getInstance().logSevere(exception);
			return;
		}

		successfulRegistration = true;
		((ProjectRegistrationWizard) getWizard()).projectId = id;
		messageTitle = "New project registered!";
		messageBody = "Your new project id "
				+ id
				+ " is registered.\nYou can change it and other WatchDog settings in the Eclipse preferences."
				+ ProjectIdEnteredEndingPage.encouragingEndMessage;
	}
}
