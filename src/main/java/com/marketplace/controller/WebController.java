package com.marketplace.controller;

import java.awt.print.PrinterIOException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.marketplace.model.Application;
import com.marketplace.model.ApplicationStatus;
import com.marketplace.model.ApplicationType;
import com.marketplace.model.Company;
import com.marketplace.model.Job;
import com.marketplace.model.JobStatus;
import com.marketplace.model.Profile;
import com.marketplace.model.User;
import com.marketplace.repo.ApplicationRepo;
import com.marketplace.repo.CompanyRepo;
import com.marketplace.repo.JobRepo;
import com.marketplace.repo.ProfileRepo;
import com.marketplace.repo.UserRepo;

@RestController
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class WebController {
	private static final int NO_OF_RESULTS_PER_PAGE = 5;

	private static final String RESUME_DIR = System.getProperty("user.dir");

	Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Autowired
	JobRepo jobRepo;
	@Autowired
	UserRepo userRepo;
	@Autowired
	CompanyRepo compRepo;
	@Autowired
	ProfileRepo profileRepo;
	@Autowired
	private JavaMailSender sender;
	@Autowired
	ApplicationRepo appRepo;

	// --------------- Sanity Check --------------------
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String welcome() {
		return "Welcome to your personal Job Tracker.";
	}

	// --------------- Job seeker sign up -------------------------
	@RequestMapping(value = "/users/create", method = { RequestMethod.POST })
	public ResponseEntity<?> createUser(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {
		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String username = jobj.get("username").getAsString();
		String password = jobj.get("password").getAsString();
		String emailid = jobj.get("emailID").getAsString();

		if (username == null || password == null || emailid == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Insufficient data"), HttpStatus.BAD_REQUEST);
		}
		User user = userRepo.findByEmailid(emailid);
		Company company = compRepo.findByEmailid(emailid);
		if (user != null || company != null) {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.BAD_REQUEST.value(),
					"Emailid with " + emailid + " is already registered."), HttpStatus.BAD_REQUEST);
		}
		user = userRepo.findByUsername(username);
		company = compRepo.findByName(username);
		if (user != null || company != null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Username already taken"),
					HttpStatus.BAD_REQUEST);
		}
		Random rand = new Random();
		String code = String.format("%04d", rand.nextInt(10000));
		user = new User(username, emailid, password, code);

		userRepo.save(user);

		request.getSession().setAttribute("loggedIn", "user");
		request.getSession().setAttribute("email", emailid);

		try {
			sendEmail(emailid,
					"Dear User,\n\nThank you for registering in Job-Borad. Your verification code is "
							+ user.getVerificationcode() + ".\n\nThanks,\nJob-board",
					"Verification code from Job-Board");
			return new ResponseEntity<String>("Email sent successfully with the verification code", HttpStatus.OK);
		} catch (Exception ex) {
			return new ResponseEntity<String>("Error sending email " + ex, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ----------------- job seeker & employer verification
	// --------------------------
	@RequestMapping(value = "/users/verify", method = { RequestMethod.POST })
	public ResponseEntity<?> verifyUser(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {
		request.setCharacterEncoding("UTF-8");

		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String verificationCode = jobj.get("verificationCode").getAsString();

		// get email id from the session
		String emailid = (String) request.getSession().getAttribute("email");

		User user = userRepo.findByEmailid(emailid);
		String msg = null;
		if (user != null) {
			if (!user.getVerificationcode().equals(verificationCode)) {
				return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.UNAUTHORIZED.value(),
						"Entered verification code does not match. Try Again"), HttpStatus.UNAUTHORIZED);
			} else {
				user.setStatus(true);
				try {
					sendEmail(emailid,
							"Dear User,\n\nYour account has been verified successfully. Happy job hunting.\n\nThanks,\nJob-board",
							"Welcome to Job-Board");
					return new ResponseEntity<String>("Email sent successfully", HttpStatus.OK);

				} catch (Exception e) {
					return new ResponseEntity<String>("Error sending email " + e, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		} else {
			Company company = compRepo.findByEmailid(emailid);
			if (company != null) {
				if (!company.getVerificationcode().equals(verificationCode)) {
					return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.UNAUTHORIZED.value(),
							"Entered verification code does not match. Try Again"), HttpStatus.UNAUTHORIZED);
				} else {
					company.setStatus(true);
					msg = "User with id " + company.getEmailid() + " is verified successfully";
					try {
						sendEmail(emailid,
								"Dear Employer,\n\nYour account has been verified successfully.\n\nThanks,\nJob-board",
								"Welcome to Job-Board");
						return new ResponseEntity<String>("Email sent successfully", HttpStatus.OK);
					} catch (Exception e) {
						return new ResponseEntity<String>("Error sending email " + e, HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
			}
		}
		return new ResponseEntity<>(msg, HttpStatus.OK);
	}

	// -------------------- Employer sign up -----------------------------
	@RequestMapping(value = "/employers/create", method = { RequestMethod.POST })
	@ResponseBody
	public ResponseEntity<?> createEmployer(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {

		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();
		System.out.println(body);
		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String emailid = jobj.get("Email ID").getAsString();
		String password = jobj.get("Password").getAsString();
		String companyname = jobj.get("Company Name").getAsString();

		String website = null;
		if (jobj.get("Website") != null) {
			website = jobj.get("Website").getAsString();
		}
		String address = jobj.get("Address_Headquarters").getAsString();
		String description = jobj.get("Description").getAsString();
		String logo = jobj.get("Logo_Image_URL").getAsString();

		if (emailid == null || password == null || companyname == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Insufficient data"), HttpStatus.BAD_REQUEST);
		}

		User user = userRepo.findByEmailid(emailid);
		Company company = compRepo.findByEmailid(emailid);
		if (user != null || company != null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(),
							"Emailid with " + emailid + " is already registered, try logging in."),
					HttpStatus.BAD_REQUEST);
		}
		user = userRepo.findByUsername(companyname);
		company = compRepo.findByName(companyname);
		if (user != null || company != null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Name already registered"),
					HttpStatus.BAD_REQUEST);
		}

		Random rand = new Random();
		String code = String.format("%04d", rand.nextInt(10000));
		company = new Company(companyname, emailid, password, website, address, description, logo, code);
		compRepo.save(company);

		request.getSession().setAttribute("loggedIn", "employer");
		request.getSession().setAttribute("email", emailid);

		// String msg = null;
		try {
			sendEmail(emailid,
					"Dear Employer,\n\nThank you for registering in Job-Borad. Your verification code is "
							+ company.getVerificationcode() + ".\n\nThanks,\nJob-board",
					"Verification code from Job-Board");
			return new ResponseEntity<String>("Email sent successfully with the verification code", HttpStatus.CREATED);
		} catch (Exception ex) {
			return new ResponseEntity<String>("Error sending email " + ex, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ------------------- Job seeker profile create/ update
	// ------------------------
	@RequestMapping(value = "/userprofile/create", method = { RequestMethod.POST }) // need
//	@ResponseBody
	public ResponseEntity<?> createUserProfile(HttpServletRequest request, HttpEntity<String> httpEntity
			)
			throws UnsupportedEncodingException {

		System.out.println("in job profile user");
		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String firstname = jobj.get("First Name").getAsString();
		String lastname = jobj.get("Last Name").getAsString();
		String picture = jobj.get("LogoImgSrc").getAsString();
		String intro = jobj.get("Self-introduction").getAsString();
		String workex = jobj.get("Work Experience").getAsString();
		String education = jobj.get("Education").getAsString();
		String skills = jobj.get("Skills").getAsString();
		String phone = "669-251-9462";

		if (firstname == null || lastname == null || workex == null || education == null || skills == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Insufficient data"), HttpStatus.BAD_REQUEST);
		}

		// get email id from the session
		String emailid = (String) request.getSession().getAttribute("email");

		User user = userRepo.findByEmailid(emailid);

		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "User with id " + emailid + " not found"),
					HttpStatus.NOT_FOUND);
		}
		

		Profile profile = profileRepo.findOne(user.getUserid());
		if (profile == null) {
			// List<String> skillList = Arrays.asList(skills.split("\\,"));
			profile = new Profile(user.getUserid(), firstname, lastname, picture, intro, workex, education, skills,
					phone);
			profileRepo.save(profile);
		} else {
			profileRepo.updateProfile(firstname, lastname, picture, intro, workex, education, skills, phone,
					user.getUserid());
		}

		String msg = "Profile with userid " + user.getUserid() + "is updated successfully";
		return new ResponseEntity<>(msg, HttpStatus.OK); // need to send an
															// email
															// notification as
															// well.
	}

	/**
	 * save resume to disk
	 * @param file
	 * @param resumePath 
	 * @throws IOException 
	 */
	// ------------------ Post a job -------------------------
	@RequestMapping(value = "/jobs/post", method = { RequestMethod.POST })
	public ResponseEntity<?> postJob(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {

		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		String job_title = jobj.get("Title").getAsString();
		String desc = jobj.get("Description").getAsString();
		String skills = jobj.get("Responsibilities").getAsString();
		String location = jobj.get("Office Location").getAsString();
		int salary = jobj.get("Salary").getAsInt();

		if (job_title == null || skills == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.BAD_REQUEST.value(), "Insufficient data"), HttpStatus.BAD_REQUEST);
		}

		String emailid = (String) request.getSession().getAttribute("email");
		
		Company company = compRepo.findByEmailid(emailid);
		if (company == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Company with id " + emailid + "not found"),
					HttpStatus.NOT_FOUND);
		}
		Job job = new Job(job_title, skills, desc, location, salary, JobStatus.OPEN, company);
		jobRepo.save(job);

		try {
			sendEmail(emailid, "Dear Employer,\n\nJob with id #" + job.getJobid()
					+ " is posted successfully. Below are the job details:\nJob-Title: " + job_title + "\nSkills: "
					+ skills + "\ndescription: " + desc + "\nLocation: " + location + ".\n\nThanks,\nJob-Borad.",
					"New job posted in Job-Board");
			return new ResponseEntity<String>("Email sent successfully with the job details", HttpStatus.CREATED);
		} catch (Exception ex) {
			return new ResponseEntity<String>("Error sending email " + ex, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ----------- Update a job Content ----------
	@RequestMapping(value = "/jobs/updateContent", method = { RequestMethod.PUT })
	public ResponseEntity<?> updateJobContent(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {

		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		Long id = jobj.get("id").getAsLong();
		String job_title = jobj.get("Title").getAsString();
		String desc = jobj.get("Description").getAsString();
		String skills = jobj.get("Responsibilities").getAsString();
		String location = jobj.get("Office Location").getAsString();
		int salary = jobj.get("Salary").getAsInt();

		Job job = jobRepo.findOne(id);
		if (job == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Job with id" + id + "Not found"),
					HttpStatus.NOT_FOUND);
		}

		jobRepo.updateJobDetails(job_title, skills, desc, location, salary, id);
		String emailid = (String) request.getSession().getAttribute("email");

		try {
			sendEmail(emailid, "Dear Employer,\n\nJob with id " + job.getJobid()
					+ " is updated successfully. Below are the job details:\nJob-Title: " + job_title + "\nSkills: "
					+ skills + "\ndescription: " + desc + "\nLocation: " + location + ".\n\nThanks,\nJob-Borad.",
					"Job with id " + id + " updated in Job-Board");
			boolean sent = emailNonTerminalApplicants(job);
			System.out.println("Emails to applicants sent: "+sent);
			return new ResponseEntity<String>("Email sent successfully with the job details", HttpStatus.OK);
		} catch (Exception ex) {
			return new ResponseEntity<String>("Error sending email " + ex, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ----------- Update a job Status by employer - filled/cancelled ----------
	@RequestMapping(value = "/jobs/updateStatus", method = { RequestMethod.PUT })
	public ResponseEntity<?> updateJobStatus(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {
		// TODO: check loggedIn

		request.setCharacterEncoding("UTF-8");
		String body = httpEntity.getBody();

		// read body
		JsonElement jelem = gson.fromJson(body, JsonElement.class);
		JsonObject jobj = jelem.getAsJsonObject();
		Long id = jobj.get("id").getAsLong();
		String status_tmp = jobj.get("Status").getAsString();

		Job job = jobRepo.findOne(id);
		if (job == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Job with id" + id + "Not found"),
					HttpStatus.NOT_FOUND);
		}

		JobStatus status = JobStatus.OPEN;
		if (status_tmp.equals("FILLED")) {
			//email
			boolean areEmailsSent = emailNonTerminalApplicants(job);
			System.out.println("areEmailsSent: "+areEmailsSent);
			//updae status
			status = JobStatus.FILLED;
			List<Application> appList = appRepo.findByStatusAndJob(ApplicationStatus.PENDING, job);
			for (Application a : appList) {
				appRepo.updateApplicationStatus_JS(ApplicationStatus.FILLED, a.getApplicationid());
			}
		} else if (status_tmp.equals("CANCELLED")) {
			
			status = JobStatus.CANCELLED;
			List<Application> appList = appRepo.findByStatusAndJob(ApplicationStatus.OFFER_ACCEPTED, job);
			if (!appList.isEmpty()) {
				return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.FORBIDDEN.value(),
						"Job cannot be cancelled, one or more offers accepted."), HttpStatus.FORBIDDEN);
			} else {
				//email
				boolean areEmailsSent = emailNonTerminalApplicants(job);
				//delete
				Long deletedAppsCount = appRepo.removeByJob(job);
				System.out.println("Deleted "+String.valueOf(deletedAppsCount)+" related applications");
				
				if (areEmailsSent) {
					return new ResponseEntity<String>(
							"Job cancelled, "
									+ " related applications deleted and applicants notified" + " via email. ",
							HttpStatus.OK);
				} else {
					return new ResponseEntity<String>("Job cancelled, " 
							+ " related applications deleted. Emails not sent", HttpStatus.OK);
				}
			}
			
			

		} else {
			return new ResponseEntity<ControllerError>(new ControllerError(HttpStatus.BAD_REQUEST.value(),
					"Not a valid Status, only FILLED/ CANCELLED are allowed"), HttpStatus.BAD_REQUEST);
		}

		jobRepo.updateJobStatus(status, id);
		String emailid = (String) request.getSession().getAttribute("email");

		try {
			sendEmail(emailid,
					"Dear Employer,\n\nJob status with id " + job.getJobid() + " is updated successfully to " + status
							+ ". Below are the job details:\nJob-Title: " + job.getJobtitle() + "\ndescription: "
							+ job.getDescription() + ".\n\nThanks,\nJob-Borad.",
					"Job with id " + id + " updated in Job-Board");
			return new ResponseEntity<String>("Email sent successfully with the job details", HttpStatus.OK);
		} catch (Exception ex) {
			return new ResponseEntity<String>("Email not sent, but status updated " + ex, HttpStatus.OK);
		}

	}

	private boolean emailNonTerminalApplicants(Job job) {
		boolean areEmailsSent = true;
		ApplicationStatus[] nonTerminalStatesArr = { ApplicationStatus.PENDING, ApplicationStatus.OFFERED };
		List<ApplicationStatus> nonTerminalStates = new ArrayList<ApplicationStatus>(
				Arrays.asList(nonTerminalStatesArr));
		List<Application> sadApplications = appRepo.findByJobAndStatusIn(job, nonTerminalStates);

		for (Application sadApplication : sadApplications) {
			String sadEmail = sadApplication.getUser().getEmailid();
			System.out.println("applicationID: "+sadApplication.getApplicationid());
			System.out.println(sadEmail);
			String sadMsg = "Your profile was very impressive, however we had to cancel"
					+ " this opening due to some unforseeable circumstances. We have kept your resme on file in case a more approprite opportunity comes up."
					+ " Please have a look at other jobs at our company. We apologize for the inconvenience";
			String sadSubject = job.getJobtitle() + " position cancelled";
			try {
				sendEmail(sadEmail, sadMsg, sadSubject);
			} catch (Exception e) {
				System.out.println("Email to " + sadEmail + " not sent");
				e.printStackTrace();
				areEmailsSent = false;
			}
		}
		return areEmailsSent;
	}

	// ------- Get all the applications of job for employer ------------
	@RequestMapping(value = "/jobApplicants/{jobid}", method = { RequestMethod.GET })
	public ResponseEntity<?> getJobApplicants(HttpServletRequest request, @PathVariable("jobid") Long id)
			throws UnsupportedEncodingException {

		Job job = jobRepo.findOne(id);
		if (job == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Job with id " + id + " not found"),
					HttpStatus.NOT_FOUND);
		}

		List<Application> appList = appRepo.findByJob(job);
		List<GetApplicants> resList = new ArrayList<GetApplicants>();

		if (appList.isEmpty()) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "No applicants for the job."),
					HttpStatus.NOT_FOUND);
		}

		for (Application a : appList) {
			User u = userRepo.findOne(a.getUser().getUserid());
			Profile p = profileRepo.findOne(u.getUserid());
			GetApplicants tmp = new GetApplicants(a.getApplicationid(), a.getStatus(), p.getFirstname(),
					p.getLastname(), u.getEmailid());
			resList.add(tmp);
		}
		ResponseEntity<List<GetApplicants>> response = new ResponseEntity<List<GetApplicants>>(resList, HttpStatus.OK);
		return response;
	}

	// ------- Get all the applied jobs for user ------------
	@RequestMapping(value = "/user/getAppliedJobs", method = { RequestMethod.GET })
	public ResponseEntity<?> getAppliedJobs(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {

		String emailid = (String) request.getSession().getAttribute("email");

		User user = userRepo.findByEmailid(emailid);

		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Uers with emailid " + emailid + " not found"),
					HttpStatus.NOT_FOUND);
		}

		List<Application> appList = appRepo.findByUserAndType(user, ApplicationType.APPLIED);

		if (appList.isEmpty()) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "No applied jobs for the user."),
					HttpStatus.NOT_FOUND);
		}

		ResponseEntity<List<Application>> response = new ResponseEntity<List<Application>>(appList, HttpStatus.OK);
		return response;
	}

	// ------- Get all the interested jobs for user ------------
	@RequestMapping(value = "/user/getInterestedJobs", method = { RequestMethod.GET })
	public ResponseEntity<?> getInterestedJobs(HttpServletRequest request, HttpEntity<String> httpEntity)
			throws UnsupportedEncodingException {

		String emailid = (String) request.getSession().getAttribute("email");

		User user = userRepo.findByEmailid(emailid);

		if (user == null) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "Uers with emailid " + emailid + " not found"),
					HttpStatus.NOT_FOUND);
		}

		List<Application> appList = appRepo.findByUserAndType(user, ApplicationType.INTERESTED);

		if (appList.isEmpty()) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "No jobs are marked interested for the user."),
					HttpStatus.NOT_FOUND);
		}

		ResponseEntity<List<Application>> response = new ResponseEntity<List<Application>>(appList, HttpStatus.OK);
		return response;
	}

	
	// ----------- Job search by job seeker -----------------
	@RequestMapping(value = "/jobs/search/{searchTerm}/{companyName}/{location}/{salary}", method = {
			RequestMethod.GET })
	public ResponseEntity<?> searchJobUser(HttpServletRequest request, @PathVariable("searchTerm") String freeText,
			@PathVariable("companyName") String companyname, @PathVariable("location") String location,
			@PathVariable("salary") String salary1,
			@RequestParam(required = false, value = "number") String resultOffset) throws UnsupportedEncodingException {
		
		int[] salIn = new int[2];
		String[] inputSal = salary1.split("-");
		if(inputSal.length == 0){
			salIn[0] = 0;
			salIn[1] = 0;
		}
		else if(inputSal.length == 1){
			salIn[0] = Integer.parseInt(inputSal[0]);
			salIn[1] = 0;
		}
		else if(inputSal.length == 2){
			salIn[0] = Integer.parseInt(inputSal[0]);
			salIn[1] = Integer.parseInt(inputSal[1]);
		}
		System.out.println("i:" + salIn[0] + "-j:" + salIn[1]);
		int salmin = salIn[0];
		int salmax = salIn[1];
		
		List<Job> freeList = new ArrayList<Job>();
		List<Job> compList = new ArrayList<Job>();
		List<Job> locList = new ArrayList<Job>();
		List<Job> salMinList = new ArrayList<Job>();
		List<Job> salMaxList = new ArrayList<Job>();
		
		List<Job> res_list = new ArrayList<Job>(); // Final output list

		boolean freeFlag = false;
		boolean compFlag = false;
		boolean locFlag = false;
		boolean salMinFlag = false;
		boolean salMaxFlag = false;

		if (!freeText.equals("null")) {
			freeList = getFreeTextJobs(freeText);
			freeFlag = true;
			res_list = freeList;
		}
		if (!companyname.equals("null")) {
			compList = getCompanyNameJobs(companyname);
			compFlag = true;
			if (compList.size() > res_list.size()) {
				res_list = compList;
			}
		}
		if (!location.equals("null")) {
			locList = getLocationJobs(location);
			locFlag = true;
			if (locList.size() > res_list.size()) {
				res_list = locList;
			}
		}
		if (salmin != 0) {
			salMinList = getMinSalaryJobs(salmin);
			salMinFlag = true;
			if (salMinList.size() > res_list.size()) {
				res_list = salMinList;
			}
		}
		if (salmax != 0) {
			salMaxList = getMaxSalaryJobs(salmax);
			salMaxFlag = true;
			if (salMaxList.size() > res_list.size()) {
				res_list = salMaxList;
			}
		}
		
		if (freeFlag)
			res_list = my_intersect(res_list, freeList);
		if (compFlag)
			res_list = my_intersect(res_list, compList);
		if (locFlag)
			res_list = my_intersect(res_list, locList);
		if (salMinFlag)
			res_list = my_intersect(res_list, salMinList);
		if (salMaxFlag)
			res_list = my_intersect(res_list, salMaxList);

		if (res_list.isEmpty()) {
			return new ResponseEntity<ControllerError>(
					new ControllerError(HttpStatus.NOT_FOUND.value(), "No job match."), HttpStatus.NOT_FOUND);
		}

		// pagination hack
		if (resultOffset != null) {
			int start = Integer.parseInt(resultOffset) - 1;
			int end = start + NO_OF_RESULTS_PER_PAGE;
			end -= end % NO_OF_RESULTS_PER_PAGE;
			int size = res_list.size();

			List<Job> paginatedResults = null;
			if (start == end) {
				paginatedResults = new ArrayList<>();
			}

			if (start >= 0 && start < size) {
				// end -= end % (size+1);
				if (end > size) {
					end = size;
				}
				paginatedResults = res_list.subList(start, end);
			} else {
				paginatedResults = new ArrayList<>();
			}
			ResponseEntity<List<Job>> response = new ResponseEntity<List<Job>>(paginatedResults, HttpStatus.OK);
			return response;

		}

		ResponseEntity<List<Job>> response = new ResponseEntity<List<Job>>(res_list, HttpStatus.OK);
		return response;
	}

	// Method to search by free text
	public List<Job> getFreeTextJobs(String freeText) {
		List<String> inputList = Arrays.asList(freeText.split("\\s*,\\s*"));
		List<Job> res = new ArrayList<Job>(); // final output list
		// Search in job title
		for (String l : inputList) {
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndJobtitleContaining(JobStatus.OPEN, l));
			tmp.removeAll(res);
			res.addAll(tmp);
		}
		// Search in location
		for (String l : inputList) {
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndLocationContaining(JobStatus.OPEN, l));
			tmp.removeAll(res);
			res.addAll(tmp);
		}
		// Search in skills
		for (String l : inputList) {
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndSkillContaining(JobStatus.OPEN, l));
			tmp.removeAll(res);
			res.addAll(tmp);
		}
		// Search in company Name
		for (String l : inputList) {
			// Company company = compRepo.findByNameContaining(l);
			Company company = compRepo.findByName(l);
			// List<Job> tmp = new
			// ArrayList<>(jobRepo.findByStatusAndCompanyContaining(JobStatus.OPEN,
			// company));
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndCompany(JobStatus.OPEN, company));
			tmp.removeAll(res);
			res.addAll(tmp);
		}
		// Search in description
		for (String l : inputList) {
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndDescriptionContaining(JobStatus.OPEN, l));
			tmp.removeAll(res);
			res.addAll(tmp);
		}
		return res;
	}

	// Method to search by Company name
	public List<Job> getCompanyNameJobs(String companyname) {
		List<String> inputList = Arrays.asList(companyname.split("\\s*,\\s*"));
		List<Job> res = new ArrayList<Job>(); // final output list
		// Search in company Name
		for (String l : inputList) {
			Company company = compRepo.findByName(l);
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndCompany(JobStatus.OPEN, company));
			res.addAll(tmp);
		}
		return res;
	}

	// Method to search by Location
	public List<Job> getLocationJobs(String location) {
		List<String> inputList = Arrays.asList(location.split("\\s*,\\s*"));
		List<Job> res = new ArrayList<Job>(); // final output list
		// Search in Locations
		for (String l : inputList) {
			List<Job> tmp = new ArrayList<>(jobRepo.findByStatusAndLocationContaining(JobStatus.OPEN, l));
			res.addAll(tmp);
		}
		return res;
	}

	// Method to search by min salary
	public List<Job> getMinSalaryJobs(int salary) {
		// Search in salary
		List<Job> res = new ArrayList<>(jobRepo.findByStatusAndSalaryGreaterThan(JobStatus.OPEN, salary - 1));
		return res;
	}
	
	// Method to search by max salary
	public List<Job> getMaxSalaryJobs(int salary) {
		// Search in salary
		List<Job> res = new ArrayList<>(jobRepo.findByStatusAndSalaryLessThan(JobStatus.OPEN, salary + 1));
		return res;
	}

	// Method for sending an email
	public void sendEmail(String recepient, String msg, String subject) throws Exception {
		MimeMessage message = sender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setTo(recepient);
		helper.setText(msg);
		helper.setSubject(subject);

		sender.send(message);
	}

	public static List<Job> my_intersect(List<Job> a, List<Job> b) {
		List<Job> result = new ArrayList<Job>();
		for (Job j : a) {
			for (Job v : b) {
				if (v.getJobid() == j.getJobid())
					result.add(j);
			}
		}
		return result;
	}

	// Custom Classes
	public class GetApplicants {
		private Long id;
		private ApplicationStatus status;
		private String firstname;
		private String lastname;
		private String emailid;

		public GetApplicants(Long id, ApplicationStatus status, String firstname, String lastname, String emailid) {
			this.id = id;
			this.status = status;
			this.firstname = firstname;
			this.lastname = lastname;
			this.emailid = emailid;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ApplicationStatus getStatus() {
			return status;
		}

		public void setStatus(ApplicationStatus status) {
			this.status = status;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public String getEmailid() {
			return emailid;
		}

		public void setEmailid(String emailid) {
			this.emailid = emailid;
		}
	}
}
