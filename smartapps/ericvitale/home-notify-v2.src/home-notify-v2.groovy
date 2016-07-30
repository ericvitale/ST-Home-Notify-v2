/**
 *  Home Notify v2
 *
 *  Version 2.0.0 - 07/29/16
 *   -- Initial Build
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  You can find this SmartApp @ https://github.com/ericvitale/ST-Home-Notify/
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
    name: "${appName()}",
	namespace: "ericvitale",
	author: "ericvitale@gmail.com",
	description: "Notifies you about things going on in your home based on what mode you are in.",
    singleInstance: true,
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: false) {
        section("Create a new child app.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New Notification Automation", multiple: true)
        }
    }
}

def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
        
        section("Modes") {
        	input "modes", "mode", title: "Modes?", multiple: true, required: false, description: "Select the  modes that you want this app to monitor and execute rules within."
        }
        
        section("Arrival Window") {
        	input "useArrivalWindow", "bool", title: "Use Arrival Window?", required: true, defaultValue: false, description: "Should this app ignore an arrival?"
	        //input "arrivalModes", "mode", title: "Modes?", multiple: true, required: false, description: "Select the  modes that you want this app to ignore ."
            input "presence", "capability.presenceSensor", title: "presence", required: false, multiple: true
            input "arrivalWindow", "number", title: "Arrival Window (mins)", required: false, defaultValue: 5, range: "0..*", description: "Number of minutes to ignore an arrival."
        }
        
        section("Contact Sensor Subscriptions") {
            input "contacts", "capability.contactSensor", title: "Which?", required: false, multiple: true
            input "contactsEvents", "enum", title: "Contact Sensor Trigger", required: false, multiple: true, options: ["Open", "Closed"]
        }
        
        section("Motion Sensor Subscriptions") {
            input "motions", "capability.motionSensor", title: "Which?", required: false, multiple: true
            input "motionsEvents", "enum", title: "Motion Sensor Trigger", required: false, multiple: true, options: ["Active", "Inactive"]
        }
        
        section("Water Sensors") {
        	input "waterSensors", "capability.waterSensor", title: "Which?", required: false, multiple: true
            input "waterSensorEvents", "enum", title: "Moisture Sensor Events", required: false, multiple: true, options: ["Wet", "Dry"]
        }
        
        section("Music Players") {
        	input "players", "capability.trackingMusicPlayer", title: "Which?", required: false, multiple: true
            input "musicTrack", "text", title: "Track to Play?", required: true, defaultValue: "1"
        }
        
        section("Speech Synthesizers") {
        	input "speech", "capability.speechSynthesis", title: "Which?", required: false, multiple: true
            input "delaySpeech", "bool", title: "Delay speech?", required: false, defaultValue: false
            input "speechDelayLength", "number", title: "Seconds to delay speech?", required: true, defaultValue: 0
        }
        
        section("Alarms") {
        	input "alarms", "capability.alarm", title: "Which?", required: false, multiple: true
            input "alarmTrack", "text", title: "Track to Play?", required: true, defaultValue: "1"
        }
        
        section("Sirens") {
        	input "sirens", "capability.alarm", title: "Which?", required: false, multiple: true
            input "sirenLength", "number", title: "Play siren for N seconds.", required: true, defaultValue: 5
        }
        
        section("Electronic Notifications") {
        	input "push", "bool", title: "Send Push Notifications?", required: true, defaultValue: true
        }
        
        section("Setting") {
        	label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "ignoreFrequentEventsDuration", "number", title: "Ignore Frequent Events for (ms)?", required: true, defaultValue: 7000, range:"2000..*", description: "This settings will determine how long this app will ignore frequent events for, 2000 ms is the minimum allowed setting."
        }
	}
}

private def appName() { return "${parent ? "Notification Automation" : "Home Notify v2"}" }
private def appVersion() { return "1.0.0" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "HN -- v${appVersion} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "HN -- v${appVersion} -- Invalid Log Setting"
        }
    }
}

def installed() {   
	log.debug "Begin installed."
	initialization() 
    log.debug "End installed."
}

def updated() {
	log.debug "Begin updated()."
	unsubscribe()
	initialization()
    log.debug "End updated()."
}

def initialization() {
	log.debug "Begin initialization()."
    
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }
    
    log.debug "End initialization()."
}

def initParent() {
	log.debug "initParent()"
}

def initChild() {
	log("Begin Child initialization().", "DEBUG")
    
    unsubscribe()
    
    if(!active) {
        log("Application is not active, ignoring further initialization tasks.", "INFO")
        log("End initialization().", "DEBUG")
        return
	}
    
    log("ignoreFrequentEventsDuration = ${ignoreFrequentEventsDuration}.", "INFO")
    
	log("Selected modes = ${modes}.", "INFO")
    
    log("Current mode = ${location.mode}.", "INFO")
    
    if(location.mode in modes) {
    	setInTheMode(true)
    } else {
    	setInTheMode(false)
    }
    	
    subscribe(location, "mode", modeChangeHandler)
    
    if(inTheMode()) {
    	log("Initialized in an active mode.", "INFO")
    } else {
        log("Initialized out of mode, waiting for mode to change.", "INFO")
        return
    }
    	
	contacts.each { it->
    	log("Selected contact sensor type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    log("Selected contact sensor events ${contactsEvents}.", "INFO")
    
    if("Open" in contactsEvents && "Closed" in contactsEvents) {
    	log("Subscribing to all contact sensor events.", "INFO")
        subscribe(contacts, "contact", contactHandler)
    } else if ("Open" in contactsEvents) {
    	log("Subscribing to [Open] contact sensor events.", "INFO")
        subscribe(contacts, "contact.open", contactHandler)
    } else if ("Closed" in contactsEvents) {
    	log("Subscribing to [Closed] contact sensor events.", "INFO")
        subscribe(contacts, "contact.closed", contactHandler)
    }
    
    motions.each { it->
    	log("Selected motion sensors type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    log("Selected motion sensor events ${motionsEvents}.", "INFO")
    
    if("Active" in motionsEvents && "Inactive" in motionsEvents) {
    	log("Subscribing to all motion sensor events.", "INFO")
        subscribe(motions, "motion", motionHandler)
    } else if ("Active" in motionsEvents) {
    	log("Subscribing to [active] motion sensor events.", "INFO")
        subscribe(motions, "motion.active", motionHandler)
    } else if ("Inactive" in motionsEvents) {
    	log("Subscribing to [inactive] motion sensor events.", "INFO")
        subscribe(motions, "motion.inactive", motionHandler)
    }
    
    waterSensors.each { it->
    	log("Selected moisture sensors type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    log("Selected moisture sensor events ${waterSensorEvents}.", "INFO")
    
    if("Wet" in waterSensorEvents && "Dry" in waterSensorEvents) {
    	log("Subscribing to all moisture sensor events.", "INFO")
        subscribe(waterSensors, "water", waterSensorHandler)
    } else if ("Wet" in waterSensorEvents) {
    	log("Subscribing to [wet] moisture sensor events.", "INFO")
        subscribe(waterSensors, "water.wet", waterSensorHandler)
    } else if ("Dry" in waterSensorEvents) {
    	log("Subscribing to [dry] moisture sensor events.", "INFO")
        subscribe(waterSensors, "water.dry", waterSensorHandler)
    }
    
    players.each { it->
    	log("Selected music players type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    speech.each { it->
    	log("Selected speech synthesizers type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    log("Delay speech setting = ${delaySpeech}.", "INFO")
    log("Speech delay set to ${speechDelayLength}.", "INFO")
    
    alarms.each { it->
    	log("Selected alarms type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    sirens.each { it->
    	log("Selected sirens type = ${it.name} and label = ${it.label}.", "INFO")
    }
    
    log("The sirens will play for ${sirenLength} seconds.", "INFO")
            
	if(useArrivalWindow) {
		def arrivalText = ""
        
        presence.each { it->
        	if(arrivalText != "") {
           		arrivalText += ", "
            }
            arrivalText += it.label
        }
    	log("Using arrival window of ${arrivalWindow} minutes for ${arrivalText}.", "INFO")
        subscribe(presence, "presence", presenceHandler)
        
    } else {
    	log("Not using arrival window.", "INFO")
    }
    
    log("End child initialization().", "DEBUG")
}

def routineHandler(evt) {
    log("Begin routineHandler(evt).", "DEBUG")
    
    log("routine = ${routine}.", "DEBUG")
    log("event = ${evt.displayName}.", "DEBUG")
    
    if(routine.toLowerCase() == evt.displayName.toLowerCase()) {
    	log("Routine: ${it} matches input selection, dismissing notification.", "INFO")

     	return
    }
    log("End routineHandler(evt).", "DEBUG")
}


def waterSensorHandler(evt) {
	
    if(location.mode in modes) {
    	log("Active mode found.", "DEBUG")
    } else {
    	log("Not in correct mode, ignoring event.", "DEBUG")
        return
    }
    
    log("Event = ${evt.descriptionText}.", "DEBUG")
    
    if (!isDuplicateCommand(state.lastEvent, ignoreFrequentEventsDuration)) {
        state.lastEvent = new Date().time    
	    playMusicTrack(musicTrack)
    	playAlarmTrack(alarmTrack)
        speak(evt.descriptionText)
        activateSirens()
        if(push) {
        	sendPushNotification(evt.descriptionText)
        }
    } else {
    	log("Frequent Event: Ignoring", "DEBUG")
    }
}

def presenceHandler(evt) {
	if(evt.value == "present") {
    	state.ignoreArrival = true
        log("Arrival detected for ${evt.device}, starting arrival window.", "INFO")
        runIn(arrivalWindow * 60, endArrivalWindow)
    }
}

def modeChangeHandler(evt) {
	if(location.mode in modes) {
    	log("Mode changed to ${location.mode}, this is a selected mode.", "INFO")	
        setInTheMode(true)
    } else {
      	log("Mode changed to ${location.mode}, this is an ignored mode.", "INFO")
    	setInTheMode(false)
    }
    
	initalization()
}

def contactHandler(evt) {
	log("Begin contactHandler().", "DEBUG")
    
    if(location.mode in modes) {
    	log("Active mode found.", "DEBUG")
    } else {
    	log("Not in correct mode, ignoring event.", "DEBUG")
        return
    }
    
    if(state.ignoreArrival && useArrivalWindow) {
    	log("Within arrival window.", "INFO")
        return
    }
    
    log("Event = ${evt.descriptionText}.", "DEBUG")
    
    if (!isDuplicateCommand(state.lastEvent, ignoreFrequentEventsDuration)) {
        state.lastEvent = new Date().time    
	    playMusicTrack(musicTrack)
    	playAlarmTrack(alarmTrack)
        speak(evt.descriptionText)
        activateSirens()
        if(push) {
        	sendPushNotification(evt.descriptionText)
        }
    } else {
    	log("Frequent Event: Ignoring", "DEBUG")
    }
	log("End contactHandler().", "DEBUG")
}

def motionHandler(evt) {
	log("Begin motionHandler().", "DEBUG")
    
    if(location.mode in modes) {
    	log("Active mode found.", "DEBUG")
    } else {
    	log("Not in correct mode, ignoring event.", "DEBUG")
        return
    }
    
    if(state.ignoreArrival && useArrivalWindow) {
    	log("Within arrival window.", "INFO")
        return
    }
    
    log("Event = ${evt.descriptionText}.", "DEBUG")
    if (!isDuplicateCommand(state.lastEvent, ignoreFrequentEventsDuration)) {
        state.lastEvent = new Date().time    
	    playMusicTrack(musicTrack)
    	playAlarmTrack(alarmTrack)
        speak(evt.descriptionText)
        activateSirens()
    } else {
    	log("Frequent Event: Ignoring", "DEBUG")
    }
	log("End motionHandler().", "DEBUG")
}

def playMusicTrack(track) {
	log("Begin playMusicTrack()", "DEBUG")
    	players?.playTrack(track)
    log("End playMusicTrack()", "DEBUG")
}

def playAlarmTrack(track) {
	log("Begin playAlarmTrack()", "DEBUG")
    	alarms?.playTrack(track)
    log("End playAlarmTrack()", "DEBUG")
}

def speak(phrase) {
	log("Begin speak()", "DEBUG")
    	if(delaySpeech) {
        	state.phrase = phrase
        	runIn(speechDelayLength, delayedSpeech)
        } else {
        	speech?.speak(phrase)
        }
    log("End speak()", "DEBUG")
}

def activateSirens() {
	log("Begin activateSirens()", "DEBUG")
    sirens?.siren()
    runIn(sirenLength, turnOffSiren)
    log("End activateSirens()", "DEBUG")
}

private isDuplicateCommand(lastExecuted, allowedMil) {
    !lastExecuted ? false : (lastExecuted + allowedMil > new Date().time) 
}

def sendPushNotification(text) {
	sendPush(text)
}

def setInTheMode(val) {
	state.inTheMode = val
}

def inTheMode() {
	return state.inTheMode
}

def turnOffSiren() {
	log("Turning off sirens.", "DEBUG")
	sirens?.off()
    log("Sirens off.", "DEBUG")
    runIn(2, makeSureSirenIsOff)
}

def makeSureSirenIsOff() {
	log("Double checking sirens.", "DEBUG")
	sirens?.off()
}

def delayedSpeech() {
	speech?.speak(state.phrase)
}

def endArrivalWindow() {
	state.ignoreArrival = false
}