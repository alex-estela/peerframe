/***********************************************************
 *  PeerFrame by Alex Estela
 **********************************************************/

var PEERFRAME = {

	// internal attributes
	backendCtxRoot: "http://localhost:8080",
	getMediasURI: "/api/medias",
	getMediasRandomAttr: "?random=true",
	pingURI: "/api/tools/ping",
	deviceSetupURI: "/api/tools/deviceSetup",
	deviceSetupUpgradeVersionAttr: "?upgradeDeviceVersion=true",
	loadingMsg: "LOADING",
	errorMsg: "ERROR",
	noMediaMsg: "NO MEDIA",
	mediaDisplayTime: 8000,
	retryFrequency: 5000,
	retryTimeout: 600000,
	debugMode: false,
	trustMode: false,
	
	// param mgmt
	paramDialogObj: null,
	paramUpdateInProgress: false,
	
	// tmp
	retryTimeSoFar: 0,
	mediaCount: 0,
	
	load: function() {
		$.ajax({
			type : "GET",
			url : PEERFRAME.backendCtxRoot + PEERFRAME.getMediasURI + PEERFRAME.getMediasRandomAttr,
			dataType : "json",
			success : function(response) {

				PEERFRAME.initParam();
				
				if (!response || !response.length || response.length == 0) {
					$(".loadingContainer").html(PEERFRAME.noMediaMsg);
					$(".loadingContainer").show();
					setTimeout(function() {
						PEERFRAME.reinit();
					}, PEERFRAME.mediaDisplayTime);
				}
				else {
						
					if (PEERFRAME.debugMode) console.log("Found " + response.length + " medias");
					
					var index = 1;
					for (var m in response) {
						var media = response[m];
						var id = media.id;
						var width = media.width;
						var height = media.height;
						var creationDate = media.created;
						var city = media.locationCity;
						var country = media.locationCountry;
						var url = PEERFRAME.backendCtxRoot + PEERFRAME.getMediasURI + "/" + id;
						
						var finalHeight = $(window).height();
						if (height < finalHeight) finalHeight = height;
						var ratio = finalHeight / height;
						var finalWidth = width * ratio;
						if (finalWidth > width) {
							finalWidth = width;
							ratio = finalWidth / width;
							finalHeight = height * ratio;
						}
						finalWidth = Math.round(finalWidth);
						finalHeight = Math.round(finalHeight);
						
						var toolTip = Date.parse(creationDate.substring(0, 19)).toString("d MMMM yyyy");
						if (city) toolTip += ", " + city;
						if (country) toolTip += ", " + country;						
						toolTip += " - " + (index) + "/" + response.length;	
						console.log(toolTip);
						
						var html = "<div><div class='mediaSpacer'>&nbsp;</div><img class='mediaImg' width='" + finalWidth + "px' height='" + finalHeight + "px'/>"
						+ "<div class='mediaTxt' style='height:" + finalHeight + "px'>" + toolTip + "</div></div>";
						$(".mediaContainer").append(html);
						
						$(".mediaImg").last().attr("data-src", url);
						if (PEERFRAME.mediaCount == 0) PEERFRAME.loadMedia(0);
			
						PEERFRAME.mediaCount++;
						index++;
						//if (PEERFRAME.mediaCount > 1) break;
					}
					
					$(".loadingContainer").hide();				
					var owl = $(".mediaContainer");
					owl.owlCarousel({
						navigation: false,
						singleItem: true,
						autoPlay: PEERFRAME.mediaDisplayTime,
						lazyLoad: false,
						transitionStyle: "fade",
						pagination: false,
						center: true,
						afterAction: function(e) {
							
							var total = PEERFRAME.mediaCount;
							var current = $(".mediaContainer").data("owlCarousel") ?
								$(".mediaContainer").data("owlCarousel").owl.currentItem : 0;
							if (PEERFRAME.debugMode) console.log("Displaying " + (current+1) + "/" + total);
							
							$(".mediaImg").eq(current).css("visibility", "visible");							
							if (current > 0) {
								$(".mediaImg").eq(current-1).css("visibility", "hidden");
							}
							
							if (current+1 < total) {
								PEERFRAME.loadMedia(current+1);
							}
							else {
								if (PEERFRAME.debugMode) console.log("All medias displayed, restarting");
								$(".mediaContainer").trigger('owl.stop');
								setTimeout(function() {
									PEERFRAME.reinit();
								}, PEERFRAME.mediaDisplayTime);
							}					
						}
					});
				}
			}
		});
	},

	loadMedia: function(index) {
		$(".mediaImg")
			.eq(index)
			.on('load', function() { 
				if (PEERFRAME.debugMode) console.log("Loaded: " + index);
			})
			.on('error', function() { 
				if (!PEERFRAME.trustMode) {
					if (PEERFRAME.debugMode) console.log("Error while loading media, restarting from scratch");
					PEERFRAME.reinit();
				}
			})
			.attr("src", $(".mediaImg").eq(index).attr("data-src"));		
	},
	
	reinit: function() {
		if ($(".mediaContainer").data("owlCarousel")) 
			$(".mediaContainer").data("owlCarousel").destroy();
		$(".mediaContainer").empty();
		PEERFRAME.retryTimeSoFar = 0; 
		PEERFRAME.mediaCount = 0; 
		if (PEERFRAME.trustMode) PEERFRAME.load();
		else PEERFRAME.init();
	},
	
	init: function() {
		$(".mediaContainer").hide();
		$(".mediaTooltip").empty();
		$(".loadingContainer").html(PEERFRAME.loadingMsg);
		$(".loadingContainer").show();
		PEERFRAME.initDisplay();
	},

	initDisplay: function() {
		$.ajax({
			type: "GET",
			url: PEERFRAME.backendCtxRoot + PEERFRAME.pingURI,
			dataType: "json",
			success: function() {
				PEERFRAME.load();
			},
			error: function() {
				PEERFRAME.retryTimeSoFar += PEERFRAME.retryFrequency;
				if (PEERFRAME.retryTimeSoFar >= PEERFRAME.retryTimeout) {
					$(".loadingContainer").html(PEERFRAME.errorMsg);
					$(".loadingContainer").show();
				}
				else {
					if (PEERFRAME.debugMode) console.log("Backend not available, retrying in " + PEERFRAME.retryFrequency + " milliseconds");
					setTimeout(PEERFRAME.init, PEERFRAME.retryFrequency);
				}
			}
		});
	},
	
	initParam: function() {
		PEERFRAME.paramDialogObj = $("#paramDialog").dialog({
			autoOpen: false,
			width: 600,
			height: 400,
			modal: true
		});
		$("#paramDialogInner").tabs({
		    create: function(e, ui) {
				$("#paramCloseButton").button();
				$("#paramCloseButton").css("padding","0px");
				$("#paramCloseButton").css("border","0px");
		        $('#paramCloseButton').on("click", function(event) {
					event.stopImmediatePropagation();
		        	PEERFRAME.paramDialogObj.dialog('close');
		        });
		    }
		});
		$('#param_wifi_ssid').keyboard();
		$('#param_wifi_key').keyboard();
		$("#paramWifiUpdateButton").button();
		$("#paramWifiUpdateButton").on("click", function() {
			if (PEERFRAME.paramUpdateInProgress) return;
			PEERFRAME.paramUpdateInProgress = true;
			$("#paramCloseButton").button("disable");
			$("#paramWifiUpdateButton").button("disable");
			$("#paramVersionUpgradeButton").button("disable");
			$("#param_wifi_connected").html("checking");	
			$("#param_wifi_ip").html("checking");
			var deviceSetup = {
				wifiSSID: $("#param_wifi_ssid").val(),
				wifiKey: $("#param_wifi_key").val()
			};
			$.ajax({
				type: "PUT",
				url: PEERFRAME.backendCtxRoot + PEERFRAME.deviceSetupURI,
			    headers: { 
			    	'Accept': 'application/json',
			        'Content-Type': 'application/json' 
			    },
				dataType: "json",
				data: JSON.stringify(deviceSetup),
				success: function(response) {
					$("#param_wifi_ssid").val(response.wifiSSID);
					$("#param_wifi_key").val(response.wifiKey);
					$("#param_wifi_connected").html((response.internetConnected ? "true" : "false"));
					$("#param_wifi_ip").html(response.localIP);	
					$("#paramCloseButton").button("enable");
					$("#paramWifiUpdateButton").button("enable");
					$("#paramVersionUpgradeButton").button("enable");
					PEERFRAME.paramUpdateInProgress = false;
				}
			});
		});	
		$("#paramVersionUpgradeButton").button();
		$("#paramVersionUpgradeButton").on("click", function() {
			if (PEERFRAME.paramUpdateInProgress) return;
			PEERFRAME.paramUpdateInProgress = true;
			$("#paramCloseButton").button("disable");
			$("#paramWifiUpdateButton").button("disable");
			$("#paramVersionUpgradeButton").button("disable");		
			$("#param_version").html("upgrading, please wait");	
			$.ajax({
				type: "PUT",
				url: PEERFRAME.backendCtxRoot + PEERFRAME.deviceSetupURI + PEERFRAME.deviceSetupUpgradeVersionAttr,
			    headers: { 
			    	'Accept': 'application/json',
			        'Content-Type': 'application/json' 
			    },
				dataType: "json",
				data: JSON.stringify({}),
				success: function(response) {
					console.log("Device upgrading, should reboot at any moment, device setup cannot be updated anymore...");
				}
			});
		});			
		$(".paramButton").show();
		$(".paramButton").on("click", function(event) {
			event.stopImmediatePropagation();
			$.ajax({
				type: "GET",
				url: PEERFRAME.backendCtxRoot + PEERFRAME.deviceSetupURI,
				dataType: "json",
				success: function(response) {
					$("#param_wifi_ssid").val(response.wifiSSID);
					$("#param_wifi_key").val(response.wifiKey);
					$("#param_wifi_connected").html((response.internetConnected ? "true" : "false"));
					$("#param_wifi_ip").html(response.localIP);
					$("#param_version").html(response.applicationVersion);
					PEERFRAME.paramDialogObj.dialog("open");
				}
			});
		});
	}
};

$(document).ready(function() {
	PEERFRAME.init();	
});