/**
 * 
 */
var app = angular.module('myApp', []);
app.controller('jobSeekerSignUpCtrl', function($scope, $http, $window) {
	$scope.user = {};
    $scope.submitData = function () {
    	console.log($scope.user.name);
    	console.log($scope.user.email);
    	console.log($scope.user.password);
    	$http({
            url: '/users/create',
            method: 'POST',
            transformResponse: function (data, headersGetter, status) 
            					{ 
            						if(status=='200')
            						{	
            							console.log("sample");
            							return data;
            						}
            						else if(status=='400'){
            							console.log(data);
            							data= JSON.parse(data);					
            							return data;
            						}; 
            					
            					},
            data: {	 username: $scope.user.name , 
            		 emailID: $scope.user.email ,
					 password: $scope.user.password
            }
        }).then(function successCallback(data) 
        		{ console.log(data);
        		  $window.localStorage.setItem("userEmail", $scope.user.email);
        		  $window.localStorage.setItem("userType", "user");
        		  $window.location.href = "/UserVerification.html";
        		}, 
        		function err(data) 
        		{
        		 console.log("error"); 
        		 console.log(data.data.badRequest.msg);
        		 $scope.errorMessage = data.data.badRequest.msg;
        		});
    	}
 });

app.controller('employerDashboardCtrl', function($scope, $http, $window) {
	
	$scope.postedJobs = {};
	
	$scope.Statuses = ["OPEN", "FILLED", "CANCELLED"];
	
	$http({
        url: '/employer/jobs',
        method: 'GET',
        transformResponse: function (data, headersGetter, status) 
        					{ 
        						if(status=='403')
        						{	
        							console.log("error");
        							return data;
        						}
        						else{
        							data= JSON.parse(data);
        							return data;
        						}; 
        					
        					}
    }).then(function successCallback(data) 
    		{ 
    		console.log(data.data);
    		$scope.postedJobs = data.data;
    		}, 
    		function err(data) 
    		{
    		 console.log("error"); 
    		 console.log(data.data.badRequest.msg);
    		});
	
	$scope.viewJob = function (job){
		console.log(job);
		$window.localStorage.setItem("jobToEdit", JSON.stringify(job));
		$window.location.href = "/viewPosting.html";
	}
	
});


app.controller('postJobsCtrl', function($scope, $http, $window) {
	
	$scope.job = {};
	$scope.submitJobData = function (){ 
		
		$http({
        url: '/jobs/post/',
        method: 'POST',
        transformResponse: function (data, headersGetter, status) 
        					{ 
        						if(status=='201')
        						{	
        							console.log("success");
        							return data;
        						}
        						else{
        							data= JSON.parse(data);
        							return data;
        						}; 
        					
        					},
		data: {	Title: $scope.job.job_title , 
				Description: $scope.job.desc, 
				Responsibilities: $scope.job.skills,
			 	'Office Location': $scope.job.location,
			 	Salary: $scope.job.salary
        }
    }).then(function successCallback(data) 
    		{ 
    		console.log(data);
    		$window.location.href = "/EmployerDashboard.html";
    		}, 
    		function err(data) 
    		{
    		 console.log("error"); 
    		 console.log(data.data.badRequest.msg);
    		});
	}
});


app.controller('viewJobCtrl', function($scope, $http, $window) {
	
	$scope.job = JSON.parse($window.localStorage.getItem("jobToEdit"));
	console.log($window.localStorage.getItem("jobToEdit"));	
	console.log($scope.job);
	
	$scope.updatePositionContent = function (){ 		
		$window.location.href = "/editJob.html";
	};
	

	$scope.cancelJob = function (job){ 
		console.log("cancelled");
		console.log($scope.job);
		$http({
	        url: '/jobs/updateStatus',
	        method: 'PUT',
	        data: {	
	        		"id": job.jobid,
	        		"Status": "CANCELLED"
	        	},
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='404')
	        						{	
	        							console.log("error");
	        							return JSON.parse(data).badRequest.msg;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							return data;
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$scope.job.status = "CANCELLED";
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data.data);
	    		});
	};
	
	$scope.fillJob = function (job){ 
		console.log("filled");
		console.log($scope.job);
		$http({
	        url: '/jobs/updateStatus',
	        method: 'PUT',
	        data: {	
	        		"id": job.jobid,
	        		"Status": "FILLED"
	        	},
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='404')
	        						{	
	        							console.log("error");
	        							return JSON.parse(data).badRequest.msg;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							
	        							return data;
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$scope.job.status = "FILLED";
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data.data);
	    		});
	};
	
	$scope.viewApplicant = function (job, emailid){ 
		console.log("viewed");
		console.log(job);
		console.log(emailid);
		$http({
	        url: '/getProfile/'+ emailid,
	        method: 'GET',
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='400')
	        						{	
	        							console.log("error");
	        							return JSON.parse(data).badRequest.msg;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							
	        							return JSON.parse(data);
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$window.localStorage.setItem("applicant", JSON.stringify(data.data));
	    		$window.location.href = "/viewJobApplicant.html"
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data.data);
	    		});
	};
	
	$scope.rejectApplication = function (job, email_id){ 
		console.log("rejected");
		console.log(job);
		console.log(email_id);
		$http({
	        url: 'employer/processApplication/',
	        method: 'POST',
	        data: {	emailid: email_id,
	        		status: "REJECTED",
	        		jobid: job.jobid
	        },
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='400')
	        						{	
	        							console.log("error");
	        							return JSON.parse(data).badRequest.msg;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							return data;
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$window.alert("Applicantion Rejected Successfully");
	    		$window.location.href = "/viewPosting.html";
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data.data);
	    		});
	};
	
	$scope.offerJob = function (job, email_id){ 
		console.log("offered");
		console.log(job);
		console.log(email_id);
		$http({
	        url: 'employer/processApplication/',
	        method: 'POST',
	        data: {	emailid: email_id,
	        		status: "OFFERED",
	        		jobid: job.jobid
	        },
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='400')
	        						{	
	        							console.log("error");
	        							return JSON.parse(data).badRequest.msg;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							return data;
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$window.alert("Job has been offered to the applicant successfully");
	    		$window.location.href = "/viewPosting.html";
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data.data);
	    		});
		
	};
	
	
	$http({
        url: '/jobApplicants/' + $scope.job.jobid,
        method: 'GET',
        transformResponse: function (data, headersGetter, status) 
        					{ 
        						if(status=='404')
        						{	
        							console.log("error");
        							return JSON.parse(data).badRequest.msg;
        						}
        						else if(status=='200')
        						{
        							data= JSON.parse(data);
        							return data;
        						}
        						else{
        							data= JSON.parse(data);
        							return data;
        						}; 
        					
        					}
    }).then(function successCallback(data) 
    		{ 
    		console.log(data.data);
    		$scope.applicants = data.data;
    		$scope.disableFilled=true;
    		var i;
    		for(i=0;i<$scope.applicants.length ; i++)
    			{
    			console.log($scope.applicants[i]);
    			if($scope.applicants[i].status == "OFFER_ACCEPTED")
    				{
    				$scope.disableCancel=true;
    				$scope.disableFilled=false;
    				}
    			};
    		}, 
    		function err(data) 
    		{
    		 console.log("error");
    		 console.log(data.data);
    		});
});

app.controller('editJobCtrl', function($scope, $http, $window) {
	
	$scope.job = JSON.parse($window.localStorage.getItem("jobToEdit"));
	console.log($window.localStorage.getItem("jobToEdit"));	
	console.log($scope.job);
	
	$scope.updateContent = function (job){
		console.log("updating job");
		$http({
	        url: '/jobs/updateContent',
	        method: 'PUT',
	        data: {	
	        		"id": job.jobid,
	        		"Title": job.jobtitle,
	        		"Description": job.description,
	        		"Responsibilities": job.skill,
	        		"Office Location": job.location,
	        		"Salary": job.salary
	        	},
	        transformResponse: function (data, headersGetter, status) 
	        					{ 
	        						if(status=='404')
	        						{	
	        							console.log("error");
	        							return data;
	        						}
	        						else if(status=='200')
	        						{
	        							console.log(data);
	        							return data;
	        						}
	        						else{
	        							data= JSON.parse(data);
	        							return data;
	        						}; 
	        					
	        					}
	    }).then(function successCallback(data) 
	    		{ 
	    		console.log(data);
	    		$window.localStorage.setItem("jobToEdit",JSON.stringify(job));
	    		$window.location.href = "/editJobSuccess.html";
	    		}, 
	    		function err(data) 
	    		{
	    		 console.log("error");
	    		 console.log(data);
	    		});
		
	};
	
	
});

app.controller('editJobSuccessCtrl', function($scope, $http, $window) {
	
	$scope.job = JSON.parse($window.localStorage.getItem("jobToEdit"));
	console.log($window.localStorage.getItem("jobToEdit"));	
	console.log($scope.job);
	
	$scope.gotoDashboard = function (){ 
		$window.location.href = "/EmployerDashboard.html";
	}
});

app.controller('editEmployerProfileCtrl', function($scope, $http, $window) {
	
	$scope.gotoDashboard = function (){ 
		$window.location.href = "/EmployerDashboard.html";
	}
	$http({
        url: '/employer',
        method: 'GET',
        transformResponse: function (data, headersGetter, status) 
        					{ 
        						if(status=='404')
        						{	
        							console.log("error");
        							return data;
        						}
        						else if(status=='200')
        						{
        							console.log(data);
        							return JSON.parse(data);
        						}
        						else{
        							data= JSON.parse(data);
        							return data;
        						}; 
        					
        					}
    }).then(function successCallback(data) 
    		{ 
    		$scope.company = data.data;
    		console.log($scope.company);
    		}, 
    		function err(data) 
    		{
    		 console.log("error");
    		 console.log(data);
    		});
	
	$scope.submitCompanyData = function (){ 
		
		$http({
        url: '/employers/update/',
        method: 'PUT',
        transformResponse: function (data, headersGetter, status) 
        					{ 
        						if(status=='403')
        						{	
        							console.log("error");
        							return data;
        						}
        						else if(status=='404')
        							{
        								console.log("error");
        								return data;
        							}
        						else if(status=='200')
        						{
        							return data;
        						}; 
        					
        					},
		data: {	'Company Name': $scope.company.name,
		    	Description: $scope.company.description, 
		    	Website: $scope.company.website,
		    	Address_Headquarters: $scope.company.address,
		    	Logo_Image_URL: $scope.company.logo_image
        }
    }).then(function successCallback(data) 
    		{ 
    		console.log("success");
    		$window.alert("Update success");
    		$window.location.href = "/EmployerDashboard.html";
    		}, 
    		function err(data) 
    		{
    		 console.log("error"); 
    		});
	}
});



app.controller('viewJobApplicantCtrl', function($scope, $http, $window) {
	$scope.profile= JSON.parse($window.localStorage.getItem("applicant"));
	console.log($scope.profile);
	$scope.gotoDashboard = function (){ 
		$window.location.href = "/EmployerDashboard.html";
	}
});


