/**
 * Created by Ramkumar on 5/14/2017.
 */

function ajaxCall(method, url, data, callback) {



    var httpRequest = new XMLHttpRequest();
    httpRequest.onreadystatechange = handleResponse;
    httpRequest.open(method, url);
    httpRequest.setRequestHeader("Content-Type", "application/json");

    httpRequest.send(JSON.stringify(data));

    function handleResponse() {
        if (httpRequest.readyState == 4) {
            callback(httpRequest.status, httpRequest.responseText);
        }
    }
}

function uploadResume() 
{
	var frm = document.getElementById('fileinfo') || null;
	
	console.log(frm);
	
	if(frm == null)
		{
			alert("Resume is not available");
			window.location.href = "/JobView.html";
		}
	
	if(frm) {
		alert("applying to a job");
	   frm.action = 'jobs/view/'+localStorage.getItem("jobId")+'/applyresume';
	}
	
	
	
	
	}


function search(e) {

    e.preventDefault();

    var searchForm = e.target;
    var inputArray = searchForm.getElementsByTagName("input");

    var FreeTextSearch,SearchByCompany,SearchByLocation,SearchBySalary;

    for (var i = 0; i < inputArray.length; i++) {
        var input = inputArray[i];

        if (input.getAttribute("name") === "FreeTextSearch") {

            if(input.value === undefined || input.value === "")
                FreeTextSearch = "null";
            else
                FreeTextSearch = input.value;
        }

        if (input.getAttribute("name") === "SearchByCompany") {
            if(input.value === undefined || input.value === "")
                SearchByCompany = "null";
            else
                SearchByCompany = input.value;
        }

        if (input.getAttribute("name") === "SearchByLocation") {
            if(input.value === undefined || input.value === "")
                SearchByLocation = "null";
            else
                SearchByLocation = input.value;
        }

        if (input.getAttribute("name") === "SearchBySalary") {
            if(input.value === undefined || input.value === "")
                SearchBySalary = "0";
            else
                SearchBySalary = input.value;
        }

    }

    var url = "/jobs/search/"+FreeTextSearch+"/"+SearchByCompany+"/"+SearchByLocation+"/"+SearchBySalary;

    console.log(url);

    var searchRequestObj = {};

    ajaxCall("GET", url, null , function (status, body) {
    	var searchResponseObj = JSON.parse(body);
        if (status == 200 || status == 201) {
            
            console.log(JSON.stringify(searchResponseObj));
            console.log(searchResponseObj);
            window.location = "/searchResults.html";
            localStorage.setItem("searchResult",url);
            localStorage.setItem("searchResultObj",JSON.stringify(searchResponseObj));
            /* viewJobs(e,searchResponseObj);*/
        }
        else {
        	
            console.log("Search not done : " + status);
            console.log(searchResponseObj);
            var msg = JSON.stringify(searchResponseObj["badRequest"]["msg"]);
            console.log(msg);
            document.getElementById("errorMessage").innerHTML = '<b>'+msg+'</b>';
            document.getElementById("errorMessage").style.display = "block";
            document.getElementById("errorMessage").style.color = "red";	
            
        }

    });

}


function viewJobs() {

    var searchKeyword = localStorage.getItem("searchResult");
    var searchResponseString = localStorage.getItem("searchResultObj");

    console.log((searchResponseString));

    var searchResponseObj = JSON.parse(searchResponseString);

    $('#searchJobResults').empty();

    var length = searchResponseObj.length;

    var pageNumbers;

    if(length%5==0)
        pageNumbers = length/5;
    else
        pageNumbers = Math.floor(length/5) + 1;

    console.log(pageNumbers + " --> " + length + " --> " + length%5);
    console.log(searchResponseObj[0]);
    
    
    for (var i = 0; i < 5; i++) {

    	var obj = searchResponseObj[i];

    	
        var jobid = obj["jobid"];
        var jobtitle = obj["jobtitle"];
        var skill = obj["skill"];
        var description = obj["description"];
        var location = obj["location"];
        var salary = obj["salary"];
        var company = obj["company"];


        var searchList = '<a href="#" id = "jobTitleId" onclick = "redirectjobviewPage(' +jobid +')"><b>'+  (i+1) + ") " +
            jobtitle + '</b></a><p></p>' +
            '<p><b>Job Requestion Number : </b>'+jobid+'</p>'+
            '<p><b>Location : </b>'+location+'</p><hr>';
        
           /* '<div class="form-group">'+
            '<button type="button" id = "toggleInterested-' +jobid+'"class="btn btn-primary btn-group-vertical login-button" onclick = "toggleInterested('+jobid+')">'+
            'Mark Interested</button>&nbsp'+
            '</div>';*/
       

        $(searchList).appendTo("#searchJobResults");
        
        

    }

    console.log(pageNumbers);
    
    var startSymbol = '<a href="#">&laquo;</a>';
    $(startSymbol).appendTo("#pageResults");
    
    for (var i = 1 ; i <= pageNumbers; i++) {
    	var url = searchKeyword +'?start='+ ((i-1)*5+1);
    	console.log(url);
    	var realURL = url.substr(1);
     	console.log(realURL);
     	localStorage.setItem("searchKeyword",searchKeyword);
        var pagination = '<a href="#" onclick = "callSearch(' + i +')"><b>'+i+'</b></a>';
        $(pagination).appendTo("#pageResults");
    }

    var endSymbol =  '<a href="#">&raquo;</a>';
    $(endSymbol).appendTo("#pageResults");

}

function toggleInterested(jobId) {
	var applyRequestObj = {};
	
	console.log("Interested job is " + jobId);
	
	checkStatusSearch(jobId);
	
	checkStatusObj = localStorage.getItem("checkStatusResultObj");

    if(checkStatusObj != "null")
         checkStatusObj = JSON.parse(checkStatusObj);



    console.log(JSON.stringify(checkStatusObj));
    
    if(checkStatusObj == null || JSON.stringify(checkStatusObj) == "null")
        {
    		System.out.println("chekcing");
    		applyRequestObj["applicationType"] = "INTERESTED";
    		console.log(applyRequestObj);
        }
    else if (checkStatusObj !== "null" || checkStatusObj != null )
    	{
    		console.log(checkStatusObj + " is not null");
    	  if (checkStatusObj["type"] == "INTERESTED" )
    		  {
    		  	applyRequestObj["applicationType"] = "NULL";
    		  }
    	}
    
    console.log("*****************");
    

    applyRequestObj["Resume"] = "";
    console.log(applyRequestObj);
    
    var url = "/jobs/view/" + jobId + "/apply";
	
    
    ajaxCall("POST", url, applyRequestObj, function (status, body) {
        if (status == 200) {
        	 console.log(JSON.stringify(applyRequestObj));
            if(applyRequestObj["applicationType"] == "INTERESTED")
                console.log("Job successfully marked interested");
            else if(applyRequestObj["applicationType"] == "NULL")
                console.log("Job successfully marked not interested");
            
            checkStatusSearch(jobId);
        }
        else if (status == 403)
        {
            alert("Failed to mark the job as interested");
        }

    });
    
    
    
	
	
	
}

function checkStatusSearch(jobId)
{
	var checkStatus = "user/"+jobId+"/getApplicationStatus";
    var checkStatusRequestObj = {};
    var checkStatusResponseObj = {};
    
    ajaxCall("GET", checkStatus, checkStatusRequestObj,function (status, body) {

        if (status == 200) {

        	console.log(checkStatusResponseObj);

            if(body=="null")
            {
                localStorage.setItem("checkStatusResultObj","null");

            }
            else {
                checkStatusResponseObj = JSON.parse(body);
                //document.getElementById("Button").disabled = true;
                if(checkStatusResponseObj["type"] == "APPLIED")
                {

                	var id = "toggleInterested-"+jobId;
                	
                		document.getElementById(id).value = "Applied&Interested"
                        document.getElementById(id).disabled = true;
                	
                }
                else if (checkStatusResponseObj["type"] == "INTERESTED")

                {
                	
                	var id = "toggleInterested-"+jobId;
                 
                    document.getElementById(id).value = "Mark Not Interested"
                    document.getElementById(id).disabled = false;
                }

                else if(checkStatusResponseObj["type"] == "NULL")
                {
                	var id = "toggleInterested-"+jobId;
                	
                	document.getElementById(id).value = "Mark Interested"
                        document.getElementById(id).disabled = false;
                	
                }

                console.log(checkStatusResponseObj);
                localStorage.setItem("checkStatusResultObj",JSON.stringify(checkStatusResponseObj));
            }


        }

    });


}



function callSearch(pageNumber) {

 	var url = localStorage.getItem("searchKeyword");
 	url = url + "?number=" + ((pageNumber-1)*5+1);
 	
 	
 	
	
	console.log("called " + url);
	
    $('#searchJobResults').empty();

    var searchRequestObj = {};
    var searchResponseObj = {};

    ajaxCall("GET", url, searchRequestObj , function (status, body) {

        if (status == 200 || status == 201) {
            var searchResponseObj = JSON.parse(body);
            console.log(JSON.stringify(searchResponseObj));
        }
        else {
            console.log("Search not done : " + status);
  
        }
        
        for (var i = 0; i < searchResponseObj.length; i++) {

        	console.log("");
            var jobid = searchResponseObj[i]["jobid"];
            var jobtitle = searchResponseObj[i]["jobtitle"];
            var skill = searchResponseObj[i]["skill"];
            var description = searchResponseObj[i]["description"];
            var location = searchResponseObj[i]["location"];
            var salary = searchResponseObj[i]["salary"];
            var company = searchResponseObj[i]["company"];


            var searchList = '<a href="#" id = "jobTitleId" onclick = "redirectjobviewPage(' +jobid +')"><b>'+  (i+1) + ") " +
                jobtitle + '</b></a><p></p>' +
                '<p><b>Job Requestion Number : </b>'+jobid+'</p>'+
                '<p><b>Location : </b>'+location+'</p><hr>';

            $(searchList).appendTo("#searchJobResults");



    }
        
    });

}


function loadAppliedViewPage() {


    $('#appliedJobResults').empty();

    var url = "user/getAppliedJobs";

    var appliedRequestObj = {};
    var appliedResponseObj = {};
    ajaxCall("GET", url, appliedRequestObj, function (status, body) {
        if (status == 200) {
            appliedResponseObj = JSON.parse(body);

            console.log(appliedResponseObj);

            for (var i = 0; i < appliedResponseObj.length; i++) {

                var jobId = appliedResponseObj[i]["job"]["jobid"];
                var jobTitle = appliedResponseObj[i]["job"]["jobtitle"];
                var applicationStatus = appliedResponseObj[i]["status"];

                var appliedJobsList = '<div class="form-group">'+
                    '<div class="cols-sm-10" style="text-align: left;margin-left: 8px">'+
                    '<p id = "jobTitle-'+jobId+'"><b>'+jobTitle+'</b></p>'+
                    '<p id = "'+jobId+'">Job ID: <b>'+jobId+'</b></p>'+
                    '</div><div class="cols-sm-10" style="text-align: left;margin-left: 8px">'+
                    'Job status : '+
                    '<p id = "jobStatus-'+jobId+'"><b>'+applicationStatus+'</b></p> ' +
                    '</div>'+
                    '<div class="form-group">'+
                    '<button type="button" id = "Accept-' +jobId+'"class="btn btn-primary btn-group-horizontal login-button" onclick="acceptOffer('+jobId+')">'+
                'Accept</button>&nbsp'+
                '<button type="button" id = "Reject-' +jobId+'"class="btn btn-primary btn-group-horizontal login-button"  onclick="rejectOffer('+jobId+')">'+
                'Reject</button>&nbsp'+
                '<button type="button" id = "Cancel-' +jobId+'"class="btn btn-primary btn-group-horizontal login-button"  onclick="cancelApplication('+jobId+')">'+
                'Cancel</button>&nbsp'+
                '</div></div>';

                $(appliedJobsList).appendTo("#appliedJobResults");

                console.log("Changing button colors");

                var checktag = 'p#jobStatus-'+jobId;

                console.log(checktag);

                console.log($(checktag).text());

                if($('p#jobStatus-'+jobId).text() == "PENDING")
                {
                    document.getElementById("Accept-"+jobId).disabled = true;
                    document.getElementById("Reject-"+jobId).disabled = true;
                    document.getElementById("Cancel-"+jobId).disabled = false;
                }
                else if($('p#jobStatus-'+jobId).text() == "OFFERED")
                {
                    document.getElementById("Accept-"+jobId).disabled = false;
                    document.getElementById("Reject-"+jobId).disabled = false;
                    document.getElementById("Cancel-"+jobId).disabled = true;
                }

                else if($('p#jobStatus-'+jobId).text() == "OFFER_ACCEPTED")
                {
                    document.getElementById("Accept-"+jobId).disabled = false;
                    document.getElementById("Reject-"+jobId).disabled = false;
                    document.getElementById("Cancel-"+jobId).disabled = false;
                }
                else if($('p#jobStatus-'+jobId).text() == "OFFER_REJECTED")
                {
                    document.getElementById("Accept-"+jobId).disabled = false;
                    document.getElementById("Reject-"+jobId).disabled = false;
                    document.getElementById("Cancel-"+jobId).disabled = false;
                }
                else if($('p#jobStatus-'+jobId).text() == "CANCELLED" || $('p#jobStatus-'+jobId) == "FILLED" )
                {
                    document.getElementById("Accept-"+jobId).disabled = true;
                    document.getElementById("Reject-"+jobId).disabled = true;
                    document.getElementById("Cancel-"+jobId).disabled = true;
                }

                

            }

        }
    });





}


function loadInterestedViewPage() {

    $('#interestedJobResults').empty();

    var url = "user/getInterestedJobs";

    var interestedRequestObj = {};
    var interestedResponseObj = {};
    ajaxCall("GET", url, interestedRequestObj, function (status, body) {
      
    	if (status == 200) {
        	
        	
            interestedResponseObj = JSON.parse(body);

            console.log(interestedResponseObj);

            for (var i = 0; i < interestedResponseObj.length; i++) {

                var jobId = interestedResponseObj[i]["job"]["jobid"];
                var jobTitle = interestedResponseObj[i]["job"]["jobtitle"];
                var applicationStatus = interestedResponseObj[i]["status"];

                var appliedJobsList = '<div class="form-group" style="display: flex;flex-direction: column">'+
                    '<div class="cols-sm-10">'+

                    '<p id = "jobTitle-'+jobId+'"><b>'+jobTitle+'</b></p>'+
                    '<p id = "'+jobId+'">Job ID: '+jobId+'</p>'+
                    '</div><div class="cols-sm-10">'+
                    'Job status :'+
                    '<p id = "jobStatus-'+jobId+'"><b> '+ applicationStatus+ '</b></p> ' +
                    '</div>'+
                    '<div class="form-group">'+
                    '<button type="button" id = "NotInterested-' +jobId+'"class="btn btn-primary btn-group-vertical login-button" onclick = "interestedSection('+jobId+')">'+
                    'Mark Not Interested</button>&nbsp'+
                    '</div></div>';

                $(appliedJobsList).appendTo("#interestedJobResults");
            }

        }

        else if (status == 404)
        {
            document.getElementById("error").innerHTML = "Users currently don't have interested jobs.";
            document.getElementById("error").innerHTML += "<br><br><b> P.S Interested Jobs are jobs that are not applied by the user but marked interested </b>";
            document.getElementById("error").style.display = "block";
            return;
        }
    });





}




//load view of a particular job
function redirectjobviewPage(jobid)
{

    window.location = "/JobView.html";
    localStorage.setItem("jobId",jobid);

}

function loadjobviewPage()
{


    var jobid = localStorage.getItem("jobId");


    var url = "/jobs/view/"+jobid;

    var jobViewRequestObj = {};

    ajaxCall("GET", url , jobViewRequestObj, function (status, body) {

        if (status == 200) {

            var responseObj = JSON.parse(body);

            console.log(responseObj);

            var jobid = responseObj["jobid"];
            var jobtitle = responseObj["jobtitle"];
            var skill = responseObj["skill"];
            var description = responseObj["description"];
            var location = responseObj["location"];
            var salary = responseObj["salary"];
            var status = responseObj["status"];
            var company = responseObj["company"];

            $('p#jobTitle').text("JobTitle : "+jobtitle);
            $('p#skill').text("Skills : "+skill);
            $('p#description').text("Description : "+description);
            $('p#location').text("Location : "+location);
            $('p#salary').text("Salary : "+salary);
            $('p#company').text("Company : "+ company["name"]);

            checkStatus(jobid);

        }
    });




}


function apply(e) {

    console.log("In apply using profile function ");
    e.preventDefault();
    var jobID;

    var applyRequestObj = {};

    jobID = localStorage.getItem("jobId");

    var url = "/jobs/view/" + Number(jobID) + "/apply";

    applyRequestObj["applicationType"] = "applied";
    applyRequestObj["Resume"] = "";

    console.log(url);

    ajaxCall("POST", url, applyRequestObj, function (status, body) {

        if (status == 200) {

            console.log("Job successfully applied");

        }

        else if (status == 403)
        {
        	console.log("Job application failed");
        	 if (window.confirm('To apply for a job, you must create a profile. Press Ok to get redirected to profile creation page')) {
        	        window.location.href='/editProfile.html';
        	    }
            
        }
        
        checkStatus(jobID);
    });
}


function interested(e) {


    e.preventDefault();
    var jobID;
    jobID = localStorage.getItem("jobId");
    var checkStatusObj = {};

    console.log("Checking the status now!!!!!");

    var p = checkStatus(jobID);

    var applyRequestObj = {};
    
    $.when(p).done(	function() {

        console.log("Succesfully Checked the status now!!!!!");



        checkStatusObj = localStorage.getItem("checkStatusResultObj");

        if(checkStatusObj == "null")
            applyRequestObj["applicationType"] = "NULL";
        else
            checkStatusObj = JSON.parse(checkStatusObj);


        console.log(checkStatusObj);

        if (checkStatusObj!="null" && checkStatusObj["type"] == "INTERESTED" )
            applyRequestObj["applicationType"] = "NULL";
        else
            applyRequestObj["applicationType"] = "INTERESTED";

        
        applyRequestObj["Resume"] = "";

        var url = "/jobs/view/" + jobID + "/apply";

        console.log(url);
        console.log(JSON.stringify(applyRequestObj));


        ajaxCall("POST", url, applyRequestObj, function (status, body) {
            if (status == 200) {

                if(applyRequestObj["applicationType"] == "INTERESTED")
                    console.log("Job successfully marked interested");
                else if(applyRequestObj["applicationType"] == "NULL")
                    console.log("Job successfully marked not interested");
                checkStatus(jobID);
            }
            else if (status == 403)
            {
                console.log("Failed to mark the job as interested");
                
                if (window.confirm('To mark interested on a job, you must create a profile. Press Ok to get redirected to profile creation page')) {
        	        window.location.href='/editProfile.html';
        	    }
                
            }

        });


    });

}

function interestedSection(jobID) {



    var checkStatusObj = {};

    console.log("Checking the status in interested section now!!!!!");

    var p = checkStatusSection(jobID);

    var applyRequestObj = {};
    
    $.when(p).done(	function() {

        console.log("Succesfully Checked the status now!!!!!");



        checkStatusObj = localStorage.getItem("checkStatusResultObj");

        if(checkStatusObj == "null")
            applyRequestObj["applicationType"] = "NULL";
        else
            checkStatusObj = JSON.parse(checkStatusObj);



        console.log(checkStatusObj);
        
        if(checkStatusObj == null)
        	console.log("true");
        else
        	console.log("false");
        
        
        if(checkStatusObj == null)
            {
        		System.out.println("chekcing");
        		applyRequestObj["applicationType"] = "INTERESTED";
        		console.log(applyRequestObj);
            }
        else if (checkStatusObj !== "null" || checkStatusObj != null )
        	{
        		console.log(checkStatusObj + " is not null");
        	  if (checkStatusObj["type"] == "INTERESTED" )
        		  {
        		  	applyRequestObj["applicationType"] = "NULL";
        		  }
        	}
        

        applyRequestObj["Resume"] = "";

        var url = "/jobs/view/" + jobID + "/apply";

        console.log(url);
        console.log((applyRequestObj));


        ajaxCall("POST", url, applyRequestObj, function (status, body) {
            if (status == 200) {
            	 console.log(JSON.stringify(applyRequestObj));
                if(applyRequestObj["applicationType"] == "INTERESTED")
                    console.log("Job successfully marked interested");
                else if(applyRequestObj["applicationType"] == "NULL")
                    console.log("Job successfully marked not interested");
                
                checkStatusSection(jobID);
            }
            else if (status == 403)
            {
                alert("Failed to mark the job as interested");
            }

        });


    });

}




function checkStatusSection(jobReq) {

    console.log("In check status section function!!!!!");

    var checkStatus = "user/"+jobReq+"/getApplicationStatus";
    var checkStatusRequestObj = {};
    var checkStatusResponseObj = {};

    ajaxCall("GET", checkStatus, checkStatusRequestObj,function (status, body) {

        if (status == 200) {


            if(body=="null")
            {
                localStorage.setItem("checkStatusResultObj","null");

            }
            else {
                checkStatusResponseObj = JSON.parse(body);
                console.log(checkStatusResponseObj);
                localStorage.setItem("checkStatusResultObj",JSON.stringify(checkStatusResponseObj));
                location.reload();
                
                
            }


        }

    });

}


function checkStatus(jobReq) {

    console.log("In check status function!!!!!");

    var checkStatus = "user/"+jobReq+"/getApplicationStatus";
    var checkStatusRequestObj = {};


    ajaxCall("GET", checkStatus, checkStatusRequestObj,function (status, body) {

        if (status == 200) {


            if(body=="null")
            {
                localStorage.setItem("checkStatusResultObj","null");

            }
            else {
                checkStatusResponseObj = JSON.parse(body);
                //document.getElementById("Button").disabled = true;
                if(checkStatusResponseObj["type"] == "APPLIED")
                {

                    document.getElementById("applybtn").disabled = true;
                    document.getElementById('applybtn').value = "Already Applied"
                    document.getElementById("markinterestedbtn").disabled = true;
                    document.getElementById('markinterestedbtn').value = "Interested"

                }
                else if (checkStatusResponseObj["type"] == "INTERESTED")

                {
                    document.getElementById('applybtn').value = "Apply"
                    document.getElementById("applybtn").disabled = false;
                    document.getElementById('markinterestedbtn').value = "Mark Not Interested"
                    document.getElementById("markinterestedbtn").disabled = false;
                }

                else if(checkStatusResponseObj["type"] == "NULL")
                {
                    document.getElementById('applybtn').value = "Apply"
                    document.getElementById('markinterestedbtn').value = "Mark Interested"
                    document.getElementById("markinterestedbtn").disabled = false;
                    document.getElementById("applybtn").disabled = false;

                }

                console.log(checkStatusResponseObj);
                localStorage.setItem("checkStatusResultObj",JSON.stringify(checkStatusResponseObj));
            }


        }
        
       

    });



}

function acceptOffer(jobReq)
{

    alert("In accept offer function!!!!!");

    var url = "user/processApplication/";
    var offerStatusObj = {};


    offerStatusObj["jobid"] = jobReq;
    offerStatusObj["status"] ="OFFER_ACCEPTED";




    ajaxCall("POST", url, offerStatusObj , function (status, body) {

        if (status == 200) {
            alert("Offer accepted!!!");
        }
        location.reload();
    });
}

function rejectOffer(jobReq)
{
    alert("In reject offer function!!!!!");

    var url = "user/processApplication/";
    var offerStatusObj = {};


    offerStatusObj["jobid"] = jobReq;
    offerStatusObj["status"] ="OFFER_REJECTED";




    ajaxCall("POST", url, offerStatusObj , function (status, body) {

        if (status == 200) {
            alert("Offer rejected!!!");
        }
        location.reload();
    });
}

function cancelApplication(jobReq)
{
    alert("In cancel application function!!!!!");

    var url = "user/processApplication/";
    var offerStatusObj = {};


    offerStatusObj["jobid"] = jobReq;
    offerStatusObj["status"] ="CANCELLED";




    ajaxCall("POST", url, offerStatusObj , function (status, body) {

        if (status == 200) {
            alert("Application cancelled!!!");
        }
        location.reload();
    });
}


function editProfileEvent(e)
{

    e.preventDefault();



    var editProfile = e.target;
    var inputArray = editProfile.getElementsByTagName("input");
    var searchRequestObj = {};
    for (var i = 0; i < inputArray.length; i++) {
        var input = inputArray[i];

        if (input.getAttribute("name") === "fName") {
            searchRequestObj["First Name"] = input.value;
        }

        if (input.getAttribute("name") === "lName") {
            searchRequestObj["Last Name"] = input.value;
        }

        if (input.getAttribute("name") === "selfIntro") {
            searchRequestObj["Self-introduction"] = input.value;
        }

        if (input.getAttribute("name") === "workEx") {
            searchRequestObj["Work Experience"] = input.value;
        }

        if (input.getAttribute("name") === "education") {
            searchRequestObj["Education"] = input.value;
        }

        if (input.getAttribute("name") === "skills") {
            searchRequestObj["Skills"] = input.value;
        }

        if (input.getAttribute("name") === "resume") {
            searchRequestObj["Resume"] = input.value;
        }
        
        if (input.getAttribute("name") === "phone") {
            searchRequestObj["Phone"] = input.value;
        }
        

        var logoImgSrc = document.getElementById("uploaded").src;
        searchRequestObj["LogoImgSrc"] = logoImgSrc;

    }

    var url = "/userprofile/create";

    ajaxCall("POST", url, searchRequestObj, function (status, body) {

        if (status == 201) {
            
            window.location.href = "/Dashboard.html";
        }

        else if (status == 200) {
            
            window.location.href = "/Dashboard.html";
        }

    });

}








