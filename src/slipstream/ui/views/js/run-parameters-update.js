/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */

//
// Updated runtime parameters on a cyclic basis
//

String.prototype.trim = function() {
   return this.replace(/^\s+|\s+$/g,"");
}

$(document).ready(function() {

    updateDashboard();

	$('input[value="Terminate"]').click(function(event){
		event.preventDefault();
		SS.hideError();
		background.fadeOutTopWindow();
		$('#terminaterundialog').dialog('open');
		return false;
	});

	$('#terminaterundialog').dialog({
		autoOpen: false,
		title: 'Terminate Virtual Machines',
		buttons: {
			"Terminate": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
				showSubmitMessage();
				$.delete_(window.location.pathname, success=function() {
					window.location.href=window.location.href;
				});
			},
			"Cancel": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
			},
		}
	});

})

var dashboardUpdater = {

	initialState: 'Inactive',
	nodesInfo: {},

	encodeName: function(parameterName) {
	    return parameterName.replace(/:/g, '\\:').replace(/\./g, '\\.');
	},

	getRuntimeValue: function(nodeName, parameterName) {
	    var encodedId = this.encodeName('#' + nodeName + ':' + parameterName);
		return $(encodedId).text();
	},

	getGlobalRuntimeValue: function(parameterName) {
	    var encodedId = this.encodeName("#ss:" + parameterName);
		return $(encodedId).text();
	},

	getRuntimeValueFullName: function(parameterName) {
		return $("#" + parameterName).text();
	},

	isAbort: function(nodeName) {
	    if(nodeName){
    		return !(this.getRuntimeValue(nodeName, 'abort') === "");
	    } else {
    		return !(this.getGlobalRuntimeValue('abort') === "");
	    }
	},

    // VM active
	isActive: function(vmState) {
	    var activeStates = ["running", "on"];
	    var lowerVmState = vmState.toLowerCase();
	    var active = $.inArray(lowerVmState, activeStates) > -1;
		return active;
	},

	nodeNodeCssClass: function(nodeName) {
		var globalAbort = !(this.getGlobalRuntimeValue('state') === "");
		var abort = false;
		if(globalAbort) {
			// find if vms under this node are the cause
			var multiplicity = this.getRuntimeValue(nodeName + ".1", "multiplicity");
			for(var i=1;i<=multiplicity;i++) {
				if(this.isAbort(nodeName + "." + i)) {
					abort = true;
					break;
				}
			}
		}
		return "dashboard-icon dashboard-node " + ((abort) ? 'dashboard-error' : 'dashboard-ok');
	},

    isUrlProperty: function(propertyName) {
        var pattern = /^[^:]+:url\..*$/;
        return pattern.test(propertyName);
    },

    updateProperty: function(propertyName, value) {
		var encodedId = this.encodeName('#' + propertyName);
		var valueTd = $(encodedId);
		if(this.isUrlProperty(propertyName)) {
		    if (value !== '') {
                var anchor = '<a href="' + value + '">' + value + '</a>';
                console.log('Set LINK property: ' + propertyName + ' to ' + anchor);
                $(valueTd).html(anchor);
		    } else {
                console.log('Set LINK property: ' + propertyName + ' with empty string');
                $(valueTd).text(value);
		    }
		} else {
		    console.log('Set NON-LINK property: ' + propertyName + ' to ' + value);
		    $(valueTd).text(value);
		}
    },

	extractNodeName: function(vmname) {
		return vmname.split('.')[0];
	},

	updateCompletedNodesInfo: function(nodename, completed) {
		this.nodesInfo[nodename] = this.nodesInfo[nodename] || {};
		var noOfCompleted = this.nodesInfo[nodename].completed || 0;
		if(completed === 'true') {
			noOfCompleted++;
		}
		this.nodesInfo[nodename].completed = noOfCompleted;
	},

	setMultiplicityNodesInfo: function(nodename, multiplicity) {
		this.nodesInfo[nodename].multiplicity = multiplicity;
	},

	getIdPrefix: function(name) {
		return "dashboard-" + name;
	},

	getCssClass: function(abort) {
		return "dashboard-icon dashboard-image " + ((abort) ? 'dashboard-error' : 'dashboard-ok');
	},

    // power icon reflecting if the vm is on/running
	getActiveCssClass: function(vmState) {
		return "vm " + (this.isActive(vmState) ? 'vm-active' : 'vm-inactive');
	},

    updateVm: function(params) {
		// Update node info (to display the (x/y) in the node dashboard box)
		var vmname = params.name;
		var nodename = this.extractNodeName(vmname);
		this.updateCompletedNodesInfo(nodename, params.completed);
		if(params.name.endsWith('.1')) {
			this.setMultiplicityNodesInfo(nodename, params.multiplicity);
		}

		var idprefix = this.escapeDot(this.getIdPrefix(params.name));

        $('#' + idprefix + '-state').text("State: " + params.state);
        $('#' + idprefix + '-statecustom').text(params.statecustom);

        // Set the icon
        $('#' + idprefix).attr('class', this.getCssClass(params.abort));
        $('#' + idprefix + " ul").attr('class', this.getActiveCssClass(params.vmstate));
    },

	updateNode: function(nodename) {
		var idprefix = this.getIdPrefix(nodename);
		var nodeinfo = this.nodesInfo[nodename];
        $('#' + idprefix + '-ratio').text("State: " + this.getRuntimeValue(nodename + '.1', 'state') + " (" + nodeinfo.completed + "/" + nodeinfo.multiplicity + ")");
        // Set the icon
        $('#' + idprefix).attr('class', this.nodeNodeCssClass(nodename));
	},

	updateOchestrator: function(nodename) {
		var idprefix = this.getIdPrefix(nodename);
        $('#' + idprefix + '-state').text("State: " + this.getRuntimeValue(nodename, 'state'));
        $('#' + idprefix).attr('class', this.getCssClass(this.isAbort(nodename)));
	},

	truncate: function(message) {
	    var maxStringSize = 18;
	    if (message.length > maxStringSize) {
	        var firstPart = message.substr(0, maxStringSize / 2 - 2);
	        var lastPart = message.substr(message.length - maxStringSize / 2 + 2, message.length - 1);
	        message = firstPart + '...' + lastPart;
	    }
		return message;
	},

    escapeDot: function(value) {
        return value.replace(/\./g, '\\.');
    },

	buildParamsFromXmlRun: function(vmname, run) {
		var params = {};
		var escapedVmName = this.escapeDot(vmname);
		var prefix = "runtimeParameter[key='" + escapedVmName + ":";
		params.name = vmname;
		params.abort = $(run).find(prefix + "abort']").text();
		params.state = $(run).find(prefix + "state']").text();
		params.statemessage = $(run).find(prefix + "statemessage']").text();
		params.statecustom = this.truncate($(run).find(prefix + "statecustom']").text());
		params.vmstate = $(run).find(prefix + "vmstate']").text();
		params.completed = $(run).find(prefix + "complete']").text();
		params.multiplicity = $(run).find(prefix + "multiplicity']").text();
		return params;
	},

	buildParamsFromLocalRun: function(vmname) {
		var params = {};
		params.name = vmname;
		prefix = this.encodeName('#' + vmname + ':');
		params.abort = $(prefix + "abort").text();
		params.state = $(prefix + "state").text();
		params.statemessage = $(prefix + "statemessage").text();
		params.statecustom = $(prefix + "statecustom").text();
		params.vmstate = $(prefix + "vmstate").text();
		params.completed = $(prefix + "completed").text();
		params.multiplicity = $(prefix + "multiplicity").text();
		return params;
	},

    updateDashboard: function() {

        var that = this;

        var callback = function(data, textStatus, jqXHR) {

	        var run = $(data).find("run");
			that.nodesInfo = {};

	        // Update general status and header
	        var newStatus = $(run).attr('state');
	        $('#state').text(newStatus);
            $("#header-title-desc").text("State: " + newStatus);

            var headerTitle = $('#header-title');
            if(that.isAbort()) {
                headerTitle.addClass('dashboard-error');
            } else {
                headerTitle.removeClass('dashboard-error');
            }

            // Update the global deployment link.
            var linkDiv = $('#header-title-link');
            var serviceLink = that.getGlobalRuntimeValue('url.service');
            if(serviceLink !== undefined && serviceLink !== '') {
                console.log('setting global service link to ' + serviceLink);
                linkDiv.html('<a href="' + serviceLink + '"></a>');
                linkDiv.attr('class', 'url-service-set');
            } else {
                linkDiv.attr('class', 'url-service-unset');
                linkDiv.empty();
            }

	        var runtimeParameters = $(run).find('runtimeParameter');
			runtimeParameters.each(function (i, parameter) {
                var key = $(parameter).attr('key');
                var value = $(parameter).text();
                that.updateProperty(key, value);
	        });

			var nodeNames = $(run).attr('nodeNames');
            nodeNames = nodeNames.split(', ');

            for (var i in nodeNames) {
                var vmname = nodeNames[i].trim();
                if(vmname === "") {
                    continue;
                }
                var params = that.buildParamsFromXmlRun(vmname, run);
                that.updateVm(params)
            }

            for (var nodename in that.nodesInfo) {
                if(nodename.startsWith('orchestrator-')) {
                    that.updateOchestrator(nodename);
                } else if(nodename != 'machine'){ // machine doesn't have node
                    that.updateNode(nodename);
                }
            }
        };

		$.get(location.href, callback, 'xml');
    }
}

function updateReports() {
	// force reload, since reports might have updated since
	// last time
	var iframe = $('#reports > iframe');
	var url = iframe.attr('src');
	iframe.attr('src',url);
}

function updateDashboard() {
    dashboardUpdater.updateDashboard();
    updateReports();
    setTimeout("updateDashboard()", 10000);
}
