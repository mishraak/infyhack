/**
 * Created by Ramkumar on 5/14/2017.
 */

var signUpRequestObj = {};
var loginRequestObj = {};
var verifyRequestObj = {};

function navigateToUrl(e) {
    e.preventDefault();
    var source = event.target || event.srcElement;
    window.location=source.value;
}

function openNav() {
    document.getElementById("mySidenav").style.width = "250px";
    document.getElementById("main").style.marginLeft = "250px";
    document.body.style.backgroundColor = "rgba(0,0,0,0.4)";
}

function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
    document.getElementById("main").style.marginLeft= "0";
    document.body.style.backgroundColor = "white";
}

function  verifyUser(e) {
    e.preventDefault();
    var vCode;
    var verifyForm = e.target;
    var inputArray = verifyForm.getElementsByTagName("input");
    var input = inputArray[0];

    if(input.getAttribute("name")==="vCode") {
        verifyRequestObj["verificationCode"] = input.value;
        vCode = input.value;
    }

    ajaxCall("POST", "/users/verify", verifyRequestObj, function (status, body) {
      
    	if (status == 200) {
          
    		var userType = localStorage.getItem("userType")

            if(userType == "employer")
            		window.location.href = "/index.html";
            else if (userType == "user")
                window.location.href = "/index.html";

        } else if (status == 403) {
        	 window.location.href = "/UserVerification.html?errorMessage=wrongCode";
             return;
        }
    });
}

function signup(e) {
    e.preventDefault();
    var error = document.getElementById("error");
    error.style.visibility = "none";
    var email, address, Name, password, CompanyDescription,LogoImg,Website ;
    var errorMessage = "";

    var signupForm = e.target;
    var inputArray = signupForm.getElementsByTagName("input");

    for (var i = 0; i < inputArray.length; i++) {

        var input = inputArray[i];

        if (input.getAttribute("name") === "name") {
            signUpRequestObj["Company Name"] = input.value;
            Name = input.value;
        }

        if (input.getAttribute("name") === "email") {
            email = input.value;
            signUpRequestObj["Email ID"] = input.value;
        }

        if (input.getAttribute("name") === "password") {
            signUpRequestObj["Password"] = input.value;
            password = input.value;
        }

        if (input.getAttribute("name") === "address") {
            signUpRequestObj["Address_Headquarters"] = input.value;
            address=input.value;
        }

        if (input.getAttribute("name") === "description") {
            signUpRequestObj["Description"] = input.value;
            CompanyDescription=input.value;
        }
        
        if (input.getAttribute("name") === "website") {
            signUpRequestObj["Website"] = input.value;
            Website=input.value;
        }

        if (input.getAttribute("name") === "logo") {
            signUpRequestObj["Logo_Image_URL"] = input.value;
            LogoImg=input.value;
        }
        
        


    }

    console.log(email.indexOf("@"));

    //do validation
    if (Name === undefined || Name === "") {
        errorMessage = "Enter your last name."
        error.innerHTML = errorMessage;
        error.style.display = "block";
        inputArray[1].focus();
        return;
    }
    else if (email === undefined || email.indexOf("@") === -1) {
        errorMessage = "Enter email in proper format."
        error.innerHTML = errorMessage;
        error.style.display = "block";
        inputArray[2].focus();
        return;
    }
    else if (password === undefined || password === "") {
        errorMessage = "Enter your password"
        error.innerHTML = errorMessage;
        error.style.display = "block";
        inputArray[3].focus();
        return;

    } else if (CompanyDescription === undefined || CompanyDescription === "") {
        errorMessage = "Enter your company's description."
        error.innerHTML = errorMessage;
        error.style.display = "block";
        inputArray[4].focus();
        return;
    }
    
    else if (Website === undefined || Website === "")
    {
        errorMessage = "Enter your company's website."
        error.innerHTML = errorMessage;
        error.style.display = "block";
        inputArray[5].focus();
        return;
    }


    signUpRequestObj["usertype"] = "company";
    //store the object in session storage temporarily
    sessionStorage.setItem("signUpRequestObj", JSON.stringify(signUpRequestObj));

    ajaxCall("POST", "/employers/create", signUpRequestObj, function (status, body) {
       
    	console.log(status);
    	
    	if (status == 201) {
            //get userToken in auth response
            /*var responseObj = JSON.parse(body);

            if (responseObj.message === "emailexists") {
                window.location.href = "Company_SignUp.html?errorMessage=" + responseObj.message;
                return;
            }*/
            
  
        	 localStorage.setItem("userEmail", signUpRequestObj["Email ID"]);
             localStorage.setItem("userType","employer");

            console.log("Employer sign up successful")
            
            window.location.href = "/UserVerification.html";
            
        } else {
        	console.log("Employer sign up fail")
            window.location.href = "/Company_SignUp.html";
        }
    });

}

function getAllUrlParams(url) {

    // get query string from url (optional) or window
    var queryString = url ? url.split('?')[1] : window.location.search.slice(1);

    console.log("URL to parse is : "+queryString);
    
    // we'll store the parameters here
    var obj = {};

    // if query string exists
    if (queryString) {

        // stuff after # is not part of query string, so get rid of it
    	queryString = queryString.split('&')[0];
        
       // console.log(queryString);

        // split our query string into its component parts
        var arr = queryString.split('&');
        	console.log("Arr is ");
        	console.log(arr);
        	
        for (var i = 0; i < arr.length; i++) {
            // separate the keys and the values
            var a = arr[i].split('=');

            // in case params look like: list[]=thing1&list[]=thing2
            var paramNum = undefined;
            var paramName = a[0].replace(/\[\d*\]/, function (v) {
                paramNum = v.slice(1, -1);
                return '';
            });

            // set parameter value (use 'true' if empty)
            var paramValue = typeof(a[1]) === 'undefined' ? true : a[1];

//                        // (optional) keep case consistent
//                        paramName = paramName.toLowerCase();
//                        paramValue = paramValue.toLowerCase();

            // if parameter name already exists
            if (obj[paramName]) {
                // convert value to array (if still string)
                if (typeof obj[paramName] === 'string') {
                    obj[paramName] = [obj[paramName]];
                }
                // if no array index number specified...
                if (typeof paramNum === 'undefined') {
                    // put the value on the end of the array
                    obj[paramName].push(paramValue);
                }
                // if array index number specified...
                else {
                    // put the value at that index number
                    obj[paramName][paramNum] = paramValue;
                }
            }
            // if param name doesn't exist yet, set it
            else {
                obj[paramName] = paramValue;
            }
        }
    }

    console.log(JSON.stringify(obj));
    
    return obj;
}

function login(e) {


    e.preventDefault();
    var loginForm = e.target;
    var inputArray = loginForm.getElementsByTagName("input");

    for (var i = 0; i < inputArray.length; i++) {
        var input = inputArray[i];

        if (input.getAttribute("name") === "email") {
            loginRequestObj["Email ID"] = input.value;

        }

        if (input.getAttribute("name") === "password") {
            loginRequestObj["Password"] = input.value;
        }

    }
    

	
	ajaxCall("POST", "/login", loginRequestObj, function (status, body) {
        
    	if (status == 200) {
    		
            localStorage.setItem("userEmail", loginRequestObj["Email ID"]);
            localStorage.setItem("userType", body);

            if(body == "employer")
            		window.location.href = "/EmployerDashboard.html";
            else if (body == "user")
            		window.location.href = "/Dashboard.html";
        }

        else if (status == 403) {


            window.location.href = "/index.html?errorMessage=wrongcredentials";
            return;
        }

        else if (status == 404) {


            window.location.href = "/index.html?errorMessage=emailIdNotFound";
            return;
        }


        else {
            window.location.href = "/index.html";
        }
    });
}



function logout(e) {
	
    e.preventDefault();

    var url = "/logout";

    var logoutRequestObj = {};

    ajaxCall("GET", url, logoutRequestObj, function (status, body) {
    	
    	console.log(status);
        if (status == 200) {
            localStorage.setItem("userEmail", null);
            localStorage.setItem("userType", null);
            window.location.href = "/index.html";
        } else {
            console.log("Logout not done : " + status);
        }
    });
}




