package com.marketplace.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javassist.expr.NewArray;
import javassist.tools.web.Webserver;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.marketplace.model.Application;
import com.marketplace.model.ApplicationStatus;
import com.marketplace.model.ApplicationType;
import com.marketplace.model.Company;
import com.marketplace.model.Job;
import com.marketplace.model.Profile;
import com.marketplace.model.User;
import com.marketplace.repo.ApplicationRepo;
import com.marketplace.repo.CompanyRepo;
import com.marketplace.repo.JobRepo;
import com.marketplace.repo.ProfileRepo;
import com.marketplace.repo.UserRepo;
import com.mysql.jdbc.Field;

@RestController
public class OtherController {
	private static final String RESUME_DIR = System.getProperty("user.dir") + "/resumes";
	private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Autowired
	JobRepo jobRepo;
	@Autowired
	UserRepo userRepo;
	@Autowired
	CompanyRepo compRepo;
	@Autowired
	ProfileRepo profileRepo;
	@Autowired
	ApplicationRepo appRepo;
	@Autowired
	private JavaMailSender sender;

	/**
	 * 5) Employer/User Login [POST request]
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "/login", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> login(HttpServletRequest request, HttpEntity<String> httpEntity) throws IOException {
		boolean isUser = false;
		HttpStatus responseStatus = null;
		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String emailid = jobj.get("Email ID").getAsString();
		String pwd = jobj.get("Password").getAsString();

		// String emailid = request.getParameter("Email ID");
		// String pwd = request.getParameter("Password");
		System.out.println(request.getHeader("Content-Type"));
		System.out.println(request.toString());
		System.out.println(emailid + " ---> " + pwd);
		User user = userRepo.findByEmailid(emailid);
		// TODO: check if verified
		if (user != null) {
			if (pwd.equals(user.getPassword())) {
				if (!user.isStatus()) { // did not verify the email
					/*
					 * For testing, do: update table user as u set u.status=1;
					 */
					return new ResponseEntity<ControllerError>(
							new ControllerError(HttpStatus.FORBIDDEN.value(), "User not verified"),
							HttpStatus.FORBIDDEN);
				}

				request.getSession().setAttribute("loggedIn", "user");
				request.getSession().setAttribute("email", user.getEmailid());
				responseStatus = HttpStatus.OK;
				isUser = true;
			} else { // incorrect pwd
				responseStatus = HttpStatus.FORBIDDEN;
			}
		} else {
			Company company = compRepo.findByEmailid(emailid);
			if (company != null) {
				if (pwd.equals(company.getPassword())) {
					if (!company.isStatus()) {
						return new ResponseEntity<ControllerError>(
								new ControllerError(HttpStatus.FORBIDDEN.value(), "Employer not verified"),
								HttpStatus.FORBIDDEN);
					}

					request.getSession().setAttribute("loggedIn", "employer");
					request.getSession().setAttribute("email", company.getEmailid());
					responseStatus = HttpStatus.OK;
					isUser = false;
				} else {// incorrect pwd
					responseStatus = HttpStatus.FORBIDDEN;
				}
			} else {// company, user not found
				responseStatus = HttpStatus.NOT_FOUND;
			}
		}

		ResponseEntity<String> responseEntity = null;
		if (!responseStatus.equals(HttpStatus.FORBIDDEN)) {
			if (isUser) {
				responseEntity = new ResponseEntity<String>("user", responseStatus);
			} else {
				responseEntity = new ResponseEntity<String>("employer", responseStatus);
			}
		} else {
			responseEntity = new ResponseEntity<String>("forbidden", responseStatus);
		}

		return responseEntity;
	}

	/**
	 * LOGOUT
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/logout", method = { RequestMethod.GET })
	@ResponseBody
	public ResponseEntity<?> logout(HttpServletRequest request) {
		boolean alreadyLogged = false;
		if (request.getSession().getAttribute("loggedIn") == null) {
			alreadyLogged = true;
		}
		request.getSession().setAttribute("loggedIn", null);
		request.getSession().setAttribute("email", null);
		if (alreadyLogged) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.OK.value(), "Logged out. You were not logged in though ;P"),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.OK);
		}

	}

	/**
	 * 7) View a job from search results
	 * 
	 * @param jobId
	 * @return
	 */
	@RequestMapping(value = "/jobs/view/{jobId}", method = { RequestMethod.GET })
	public ResponseEntity<?> getJob(@PathVariable Long jobId) {
		Job job = jobRepo.findOne(jobId);
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(), "Not found"),
					HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Job>(job, HttpStatus.OK);
	}

	/**
	 * apply job
	 * 
	 * Request Body { applicationType : “interested”/ “applied” /“null” }
	 * 
	 * @param request
	 * @param jobId
	 * @return
	 */
	@RequestMapping(value = "/jobs/view/{jobId}/apply", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> applyForJob(HttpServletRequest request, @PathVariable Long jobId,
			HttpEntity<String> httpEntity, HttpServletResponse httpServletResponse) {
		Job job = jobRepo.findOne(jobId);
		// sanity checks
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("user")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "User not logged in"), HttpStatus.FORBIDDEN);
		}

		String emailid = (String) request.getSession().getAttribute("email");
		System.out.println("inside apply : Email ---> " + emailid);

		// sanity checks
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"Job with id " + String.valueOf(jobId) + " Not found"), HttpStatus.NOT_FOUND);
		}
		User user = userRepo.findByEmailid(emailid);
		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with email " + emailid + " Not found"),
					HttpStatus.NOT_FOUND);
		}
		Profile profile = profileRepo.findOne(user.getUserid());
		if (profile == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.FORBIDDEN.value(),
					"Please create your profile before applying for a job"), HttpStatus.FORBIDDEN);
		}

		ApplicationStatus status = ApplicationStatus.PENDING;
		ApplicationType type = null;

		// read body
		String body = httpEntity.getBody();
		System.out.print("Request Body: ");
		System.out.println(body);
		Application application = null;
		if (body == null || equals(body.isEmpty())) {
			type = ApplicationType.APPLIED;
		} else {
			JsonElement jelem = gson.fromJson(body, JsonElement.class);
			JsonObject jobj = jelem.getAsJsonObject();

			if (jobj.get("applicationType") != null) {
				switch (jobj.get("applicationType").getAsString().toUpperCase()) {
				case "INTERESTED":
					application = appRepo.findByJobAndUser(job, user);
					type = ApplicationType.INTERESTED;
					break;
				case "APPLIED":
					type = ApplicationType.APPLIED;
					// limit 5 applications per user
					// List<Application> userApps = appRepo.findByUser(user);
					List<Application> userApps = appRepo.findByUserAndType(user, ApplicationType.APPLIED);
					if (userApps != null && userApps.size() >= 5) {
						return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.FORBIDDEN.value(),
								"Sorry, you can only apply to 5 jobs at a time."), HttpStatus.FORBIDDEN);
					}
					break;
				case "NULL":
					// mark as not interested
					Application existingApp = appRepo.findByJobAndUser(job, user);
					// TODO: maybe assuming existingApp exists
					appRepo.delete(existingApp);
					return new ResponseEntity<String>("Marked as not interested", HttpStatus.OK);
				default:
					return new ResponseEntity<ControllerError>(
							new ControllerError(HttpStatus.BAD_REQUEST.value(),
									"\"applicationType\" can be either \"interested\" or \"applied\""),
							HttpStatus.BAD_REQUEST);
				}
			} else {
				type = ApplicationType.APPLIED;
			}
		}

		if (application == null) {
			application = new Application(user, job, type, status);
		}
		try {
			appRepo.save(application);
		} catch (DataIntegrityViolationException e) {
			application = appRepo.findByJobAndUser(job, user);
			appRepo.updateApplicationType(type, application.getApplicationid());

			// return new ResponseEntity<ControllerError>(new
			// ControllerError(HttpStatus.CONFLICT.value(),
			// "You have already applied for this job. " + "Your emailID: " +
			// emailid + ". Job ID: " + jobId),
			// HttpStatus.CONFLICT);

		}

		// email
		String subject = "Thank you for applying to " + job.getCompany().getName() + " via Job-Board";
		try {
			sendEmail(user.getEmailid(),
					"You successfully applied to the job. Below is the description.\n" + job.getDescription(), subject);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();

			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.OK.value(), "Unable to send email. But applied to job"),
					HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * 
	 * 10) Employer details Update [PUT request] Request Body
	 * 
	 * { Email ID: “”, Company Name: “”, Website:””, Address_Headquarters: ””,
	 * Description:””, Logo_Image_URL : ”” }
	 * 
	 * @param request
	 * @param companyId
	 * @return
	 */
	@RequestMapping(value = "/employers/update", method = { RequestMethod.PUT })
	@ResponseBody
	public ResponseEntity<?> updateCompany(HttpServletRequest request, HttpEntity<String> httpEntity) {
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("employer")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "Employer not logged in"), HttpStatus.FORBIDDEN);
		}
		String email = (String) request.getSession().getAttribute("email");
		Company company = compRepo.findByEmailid(email);
		if (company == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Company with emailid " + email + " Not found"),
					HttpStatus.NOT_FOUND);
		}

		// read body
		String body = httpEntity.getBody();
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();

		JsonElement name = jobj.get("Company Name");
		JsonElement website = jobj.get("Website");
		JsonElement address = jobj.get("Address_Headquarters");
		JsonElement description = jobj.get("Description");
		JsonElement logo_image = jobj.get("Logo_Image_URL");
		if (name != null)
			company.setName(name.getAsString());
		if (website != null)
			company.setWebsite(website.getAsString());
		if (address != null)
			company.setAddress(address.getAsString());
		if (description != null)
			company.setDescription(description.getAsString());
		if (logo_image != null)
			company.setLogo_image(logo_image.getAsString());

		compRepo.save(company);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * 12) get all jobs for an employer (the one logged in) [GET request]
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/employer/jobs", method = { RequestMethod.GET })
	@ResponseBody
	public ResponseEntity<?> getAllJobsForEmployer(HttpServletRequest request) {
		System.out.println("isLogged IN --> " + (String) request.getSession().getAttribute("loggedIn"));
		System.out.println((String) request.getSession().getAttribute("email"));

		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("employer")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "Employer not logged in"), HttpStatus.FORBIDDEN);
		}

		String email = (String) request.getSession().getAttribute("email");
		Company company = compRepo.findByEmailid(email);
		List<Job> jobs = company.getJobs();

		return new ResponseEntity<List<Job>>(jobs, HttpStatus.OK);
	}

	/**
	 * 16) Employer - Process Application [POST] Request:{ “emailid”:
	 * “foo@bar.com”, “jobid”: 1, “status”: “OFFERED/REJECTED” }
	 * 
	 * @param request
	 * @param httpEntity
	 * @return
	 */
	@RequestMapping(value = "/employer/processApplication", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> empProcessApplication(HttpServletRequest request, HttpEntity<String> httpEntity) {
		// TODO: verify employer, i.e make sure that the employer (loggedIn), is
		// only able to modify the jobs posted by their company
		// TODO: resolve state meanings, add FILLED state here, later - parse
		// enum from JsonObject gson
		System.out.println("isLogged IN --> " + (String) request.getSession().getAttribute("loggedIn"));
		System.out.println((String) request.getSession().getAttribute("email"));

		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("employer")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "Employer not logged in"), HttpStatus.FORBIDDEN);
		}

		// read body
		String body = httpEntity.getBody();
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();

		JsonElement emailid = jobj.get("emailid");
		JsonElement status = jobj.get("status");
		JsonElement jobid = jobj.get("jobid");

		if (emailid == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "emailid is null"), HttpStatus.BAD_REQUEST);
		}
		if (status == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "status is null"), HttpStatus.BAD_REQUEST);
		}
		if (jobid == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "jobid is null"), HttpStatus.BAD_REQUEST);
		}

		User user = userRepo.findByEmailid(emailid.getAsString());
		if (user == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"User with email ID " + emailid.getAsString() + " not found"), HttpStatus.NOT_FOUND);
		}

		Job job = jobRepo.findOne(jobid.getAsLong());
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"Job with id " + String.valueOf(jobid.getAsLong()) + " not found"), HttpStatus.NOT_FOUND);
		}
		Application application = appRepo.findByJobAndUser(job, user);
		if (application == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with email ID " + emailid
							+ " has not applied to job id " + String.valueOf(jobid.getAsLong())),
					HttpStatus.NOT_FOUND);
		}
		ApplicationStatus applicationStatus = null;
		switch (status.getAsString().toUpperCase()) {
		case "OFFERED":
			applicationStatus = ApplicationStatus.OFFERED;
			break;
		case "REJECTED":
			applicationStatus = ApplicationStatus.REJECTED;
			break;
		default:
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.BAD_REQUEST.value(),
					"application_status can only have the values: OFFERED, REJECTED in this endpoint\n"
							+ "Supplied value: " + status.getAsString()),
					HttpStatus.BAD_REQUEST);
		}
		application.setStatus(applicationStatus);
		appRepo.save(application);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * 17) User - Process Application [POST] Request:{ “application_id”: 1
	 * “status”: “CANCELLED/OFFER_ACCEPTED/OFFER_REJECTED” }
	 * 
	 * @param request
	 * @param httpEntity
	 * @return
	 */
	@RequestMapping(value = "/user/processApplication", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> userProcessApplication(HttpServletRequest request, HttpEntity<String> httpEntity) {

		System.out.println("isLogged IN --> " + (String) request.getSession().getAttribute("loggedIn"));
		System.out.println((String) request.getSession().getAttribute("email"));

		// sanity checks
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("user")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "User not logged in"), HttpStatus.FORBIDDEN);
		}
		// read body
		String body = httpEntity.getBody();
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		JsonElement jobid = jobj.get("jobid");
		JsonElement status = jobj.get("status");
		if (jobid == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "jobid is null"), HttpStatus.BAD_REQUEST);
		}
		if (status == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "status is null"), HttpStatus.BAD_REQUEST);
		}
		String emailid = (String) request.getSession().getAttribute("email");
		User user = userRepo.findByEmailid(emailid);
		Job job = jobRepo.findOne(jobid.getAsLong());
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"Job with ID " + String.valueOf(jobid.getAsLong()) + " not found"), HttpStatus.NOT_FOUND);
		}

		// retrieve application object
		Application application = appRepo.findByJobAndUser(job, user);
		if (application == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with email ID " + emailid
							+ " has not applied to job id " + String.valueOf(jobid.getAsLong())),
					HttpStatus.NOT_FOUND);
		}
		ApplicationStatus applicationStatus = null;
		switch (status.getAsString().toUpperCase()) {
		case "OFFER_ACCEPTED":
			applicationStatus = ApplicationStatus.OFFER_ACCEPTED;
			break;
		case "OFFER_REJECTED":
			applicationStatus = ApplicationStatus.OFFER_REJECTED;
			break;
		case "CANCELLED":
			applicationStatus = ApplicationStatus.CANCELLED;
			break;
		default:
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.BAD_REQUEST.value(),
					"application_status can only have the values: OFFER_ACCEPTED, OFFER_REJECTED or CANCELLED in this endpoint\n"
							+ "Supplied value: " + status.getAsString()),
					HttpStatus.BAD_REQUEST);
		}
		application.setStatus(applicationStatus);
		appRepo.save(application);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/**
	 * 20) Application Status of a job id for the logged-in user [GET] OR, get
	 * Application object
	 * 
	 * @param jobId
	 * @return
	 */
	@RequestMapping(value = "/user/{jobId}/getApplicationStatus", method = { RequestMethod.GET })
	public ResponseEntity<?> getApplicationStatus(HttpServletRequest request, @PathVariable Long jobId) {

		// sanity checks
		System.out.println("isLogged IN --> " + (String) request.getSession().getAttribute("loggedIn"));
		System.out.println((String) request.getSession().getAttribute("email"));
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("user")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "User not logged in"), HttpStatus.FORBIDDEN);
		}
		Job job = jobRepo.findOne(jobId);
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(), "Not found"),
					HttpStatus.NOT_FOUND);
		}

		String email = (String) request.getSession().getAttribute("email");
		User user = userRepo.findByEmailid(email);// loggedIn user cannot be
													// null
		Application application = appRepo.findByJobAndUser(job, user);
		if (application == null) {
			return new ResponseEntity<String>("null", HttpStatus.OK);
		}
		return new ResponseEntity<Application>(application, HttpStatus.OK);
	}

	/**
	 * 21) Get Job seeker Profile using application_id [GET]
	 * 
	 * @param request
	 * @param emailid
	 * @return profile object
	 */
	@RequestMapping(value = "/getProfile/{emailid:.+}", method = { RequestMethod.GET })
	public ResponseEntity<?> getProfile(HttpServletRequest request, @PathVariable String emailid) {
		// TODO: maybe check if user loggedIn
		System.out.println("---" + emailid + "---");
		User user = userRepo.findByEmailid(emailid);
		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with email id " + emailid + " not found"),
					HttpStatus.NOT_FOUND);
		}
		Profile profile = profileRepo.findOne(user.getUserid());
		if (profile == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(),
							"User with email id " + emailid + " has signed up, but not created a profile yet."),
					HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Profile>(profile, HttpStatus.OK);
	}

	/**
	 * 22) Get Employer (logedIn) [GET]
	 * 
	 * @param request
	 * @return Company object
	 */
	@RequestMapping(value = "/employer", method = { RequestMethod.GET })
	public ResponseEntity<?> getEmployer(HttpServletRequest request) {
		// sanity checks
		System.out.println("isLogged IN --> " + (String) request.getSession().getAttribute("loggedIn"));
		System.out.println("LoggedIn Email: " + (String) request.getSession().getAttribute("email"));
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("employer")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "Employer not logged in"), HttpStatus.FORBIDDEN);
		}
		String emailid = (String) request.getSession().getAttribute("email");
		Company company = compRepo.findByEmailid(emailid);
		if (company == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"Employer with email id " + emailid + " not found"), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<Company>(company, HttpStatus.OK);
	}

	/**
	 * apply job from resume
	 * 
	 * @param request
	 * @param jobId
	 * @return
	 * @throws URISyntaxException
	 */
	@RequestMapping(value = "/jobs/view/{jobId}/applyresume", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> applyForJobWithResume(HttpServletRequest request, @PathVariable Long jobId,
			HttpEntity<String> httpEntity, @RequestParam("file") MultipartFile resume,
			HttpServletResponse httpServletResponse) throws URISyntaxException {

		if (resume == null || resume.isEmpty()) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Please upload resume"),
					HttpStatus.BAD_REQUEST);
		}
		Job job = jobRepo.findOne(jobId);
		// sanity checks
		if (request.getSession().getAttribute("loggedIn") == null
				|| !((String) request.getSession().getAttribute("loggedIn")).equals("user")) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "User not logged in"), HttpStatus.FORBIDDEN);
		}

		String emailid = (String) request.getSession().getAttribute("email");
		System.out.println("inside apply : Email ---> " + emailid);

		// sanity checks
		if (job == null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.NOT_FOUND.value(),
					"Job with id " + String.valueOf(jobId) + " Not found"), HttpStatus.NOT_FOUND);
		}
		User user = userRepo.findByEmailid(emailid);
		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with email " + emailid + " Not found"),
					HttpStatus.NOT_FOUND);
		}
		List<Application> userApps = appRepo.findByUserAndType(user, ApplicationType.APPLIED);
		if (userApps != null && userApps.size() >= 5) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.FORBIDDEN.value(), "Sorry, you can only apply to 5 jobs at a time."),
					HttpStatus.FORBIDDEN);
		}
		Profile profile = profileRepo.findOne(user.getUserid());
		if (profile == null) {
			profile = new Profile();
		}

		String resumePath = null;
		try {
			resumePath = saveResume(resume, user.getUserid(), job.getJobid());
		} catch (IOException e) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Unable to save resume."),
					HttpStatus.BAD_REQUEST);
		}

		profile.setUserid(user.getUserid());
		resumePath = "/download?path=" + resumePath;
		profile.setResumePath(resumePath);
		ApplicationStatus status = ApplicationStatus.PENDING;
		ApplicationType type = ApplicationType.APPLIED;
		Application application = new Application(user, job, type, status);

		try {
			appRepo.save(application);
		} catch (DataIntegrityViolationException e) {
			application = appRepo.findByJobAndUser(job, user);
			appRepo.updateApplicationType(type, application.getApplicationid());

			// return new ResponseEntity<ControllerError>(new
			// ControllerError(HttpStatus.CONFLICT.value(),
			// "You have already applied for this job. " + "Your emailID: " +
			// emailid + ". Job ID: " + jobId),
			// HttpStatus.CONFLICT);

		}

		// email
		String subject = "Thank you for applying to " + job.getCompany().getName() + " via Job-Board";
		try {
			sendEmail(user.getEmailid(),
					"You successfully applied to the job. Below is the description.\n" + job.getDescription(), subject);
		} catch (Exception e) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.OK.value(), "Unable to send email. But applied to job"),
					HttpStatus.OK);
		}

		URI uri = new URI("/searchResults.html");
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setLocation(uri);
		return new ResponseEntity<>(httpHeaders, HttpStatus.SEE_OTHER);
	}

	private String saveResume(MultipartFile resume, Long userid, Long jobid) throws IOException {
		// save resume
		String userdirPath = RESUME_DIR + "/user" + String.valueOf(userid) + "_job" + String.valueOf(jobid);
		File userDir = new File(userdirPath);
		if (!userDir.exists()) {
			userDir.mkdirs();
		}
		String resumePath = userdirPath + "/" + resume.getOriginalFilename();
		saveFile(resume, resumePath);
		return resumePath;
	}

	private void saveFile(MultipartFile file, String resumePath) throws IOException {
		byte[] bytes = file.getBytes();
		Path path = Paths.get(resumePath);
		Files.write(path, bytes);
		System.out.println("file saved at " + resumePath);
	}

	// Method for sending an email
	private void sendEmail(String recepient, String msg, String subject) throws Exception {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setTo(recepient);
		helper.setText(msg);
		helper.setSubject(subject);

		sender.send(message);
	}

	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public void getDownload(HttpServletResponse response, @RequestParam String path) throws IOException {

		// String path1 =
		// "/Users/gurnoorsinghbhatia/Documents/code/sem2/cmpe275/Job-Board/Job-Board/src/main/resources/user3_job11/GoF
		// Design Patterns - Iterator (2) copy.pdf";
		// Get your file stream from wherever.
		File file = new File(path);
		InputStream myStream = new FileInputStream(file);

		// Set the content type and attachment header.
		response.addHeader("Content-disposition", "attachment;filename=" + file.getName());
		response.setContentType("txt/pdf");

		// Copy the stream to the response's output stream.
		IOUtils.copy(myStream, response.getOutputStream());
		response.flushBuffer();
	}

}
