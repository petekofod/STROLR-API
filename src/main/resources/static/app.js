Vue.config.devtools = true
var vue_det = new Vue({
    el: '#MainApp',
    data: {
        form: {
            LocoID: null,
            StartDate: null,
            EndDate: null,
            StartTime: null,
            EndTime: null,
            SCACMark: null,
            RequestType: null
        },
        lstFullTree: [{
            value: null,
            text: 'Please select an option'
        }],
        lstTimeZone:
        		[
            	{ text: 'Eastern' , value: 5},
            	{ text: 'Central' , value: 6},
            	{ text: 'Mountain' , value: 7},
            	{ text: 'Pacific' , value: 8},
            	{ text: 'UTC' , value: 0}
            	],
        status_data: {
            TestTime: null,
            IpInformation: {
              isAvailable: false,
              ATTModem: null,
              VerizonModem: null
            },
            IpReachability: {
               isAvailable: false,
               ATTModemStatus: null,
               VerizonModemStatus: null
            },
            WifiInformation: {
               isAvailable: false,
               ClientId: null,
               AccessPoint: null
            },
            GatewayInformation: {
               isAvailable: false,
               ETMSClient: null,
               MDMClient: null
            },
            RouteInformation: {
               isAvailable: false,
               ATTRoute: null,
               VerizonRoute: null,
               WifiRoute: null,
               MHzRadio: null
            },
            RadioInformation: {
              isAvailable: false,
              RadioId: null,
              EMPAddress: null
            },
        },
        show: true,
        bLogStatus: false,
        timerCheck: '',
        sStatus: '',
        sLogRequestStatus: '',
        timeUTC: '',
        userName: '',
        formTZ: '',
        messageId: '',
        sFilesCount: 'Counting...',
        sTotalBytes: 'Checking log file...',
        showLinks: false,
        showStatus:false,
        cell_class: 'table-danger',
        isActive: false,
        tabName: ''
    },
    created: function () {
        this.getUserName()
        this.fetchData()
        this.initDateTime()
        this.timeUTC = this.getUTCTime()
        this.timer = setInterval(this.getUTCTime, 5000)
    },
    methods: {
        setRequestType(value) {
            this.form.RequestType = value
        },
        onSubmit(evt) {
            evt.preventDefault()

            this.bLogStatus = true
            this.timer = setInterval(this.checkLogData, 1000)

            var xhr = new XMLHttpRequest()
            var self = this

            xhr.open('POST', 'data-request')
            xhr.setRequestHeader('Content-type', 'application/json; charset=utf-8')
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        self.sLogRequestStatus = "Request submitted successfully"
                        self.messageId = xhr.responseText
                    } else {
                        var errorMessage = JSON.parse(xhr.responseText).message
                        self.sLogRequestStatus = "Request status: ERROR while sending request! Contact the system administrator. " + errorMessage
                        console.error('error - ' + errorMessage)
                        console.error(xhr.responseText)
                    }
                }
            }
            xhr.send(JSON.stringify(this.form))
        },
        onReset(evt) {
            evt.preventDefault()
            this.form.LocoID = null
            this.form.SCACMark = null
            this.initDateTime()
            // Trick to reset/clear native browser form validation state
            this.show = false
            this.$nextTick(() => {
                this.show = true
            })
        },
        initDateTime: function () {
            this.form.StartDate = this.formatDate()
            this.form.EndDate = this.form.StartDate
            this.form.StartTime = '00:01'
            this.form.EndTime = '23:59'
        },
        fetchData: function () {
            var xhr = new XMLHttpRequest()
            var self = this
            var dataSCAC

            xhr.open('GET', 'railroads.json')
            xhr.onload = function () {
                dataSCAC = JSON.parse(xhr.responseText)
                self.lstFullTree = self.lstFullTree.concat(dataSCAC.SCAC)
            }
            xhr.send()
        },
        getDstState: function () {
        	var today = new Date();
            var jan = new Date(today.getFullYear(), 0, 1);
            var jul = new Date(today.getFullYear(), 6, 1);
            var hiTZ = Math.max(jan.getTimezoneOffset(), jul.getTimezoneOffset());
            if (today.getTimezoneOffset() < hiTZ){
            	console.log("DST detected");
            }
        },
        getUserName: function () {
            var xhr = new XMLHttpRequest()
            var self = this
            var dataUser

            xhr.open('GET', 'user.json')
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        dataUser = JSON.parse(xhr.responseText)
                        self.userName = dataUser.userName
                    } else {
                        self.userName = ''
                        console.error('error - ' + xhr.statusText);
                    }
                }
            }
            xhr.send()
        },
        checkLogData: function () {
            var xhr = new XMLHttpRequest()
            var self = this

            xhr.open('GET', 'status-update/' + this.messageId)
            xhr.onload = function () {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        statusUpdate = JSON.parse(xhr.responseText)
                        if (statusUpdate.filesCount)
                            self.sFilesCount = statusUpdate.filesCount
                        if (statusUpdate.totalBytes)
                            self.sTotalBytes = statusUpdate.totalBytes
                        if (statusUpdate.Status === "4") {
                            self.cancelLogDataCheck()
                            self.showLinks = true
                        }
                        self.sStatus = statusUpdate.statusText
                    } else if (xhr.status === 204) {
                        // no update, try again later
                    }
                    else {
                        console.error('error - ' + xhr.statusText);
                    }
                }
            }
            xhr.send()
        },
        cancelLogDataCheck() {
            clearInterval(this.timer)
        },
        formatDate: function () {
            var d = new Date(),
                month = '' + (d.getMonth() + 1),
                day = '' + d.getDate(),
                year = d.getFullYear();

            if (month.length < 2)
                month = '0' + month;
            if (day.length < 2)
                day = '0' + day;

            return [year, month, day].join('-');
        },

        getUTCTime: function () {
            // var str = 'AM'
            var d = new Date();
            var hr = d.getUTCHours();
            var min = d.getUTCMinutes();
            var res;

            // if (hr == 0) {
            //     hr = 12
            // } else
            // if (hr > 11) {
            //     hr = hr - 11
            //     str = 'PM'
            // }
            if (hr < 10) hr = '0' + hr;
            if (min < 10) min = '0' + min;

            res = hr + ":" + min // + " " + str
            this.timeUTC = res
            return res

        },
         onGetStatus(evt) {
           this.status_data.TestTime = "Tue Apr 21 11:14:02 EDT 2020"
           this.status_data.IpInformation.isAvailable = true;
           this.status_data.IpInformation.ATTModem = "10.149.1.42"
           this.status_data.IpInformation.VerizonModem = "10.148.0.33"

           this.status_data.IpReachability.isAvailable = true;
           this.status_data.IpReachability.ATTModemStatus = "Online"
           this.status_data.IpReachability.VerizonModemStatus = "Unreachable"

           if (this.status_data.IpReachability.VerizonModemStatus == "Unreachable")
               this.cell_class = 'table-danger'
           else
                this.cell_class = 'table-success'

           this.status_data.WifiInformation.ClientId = "NTD4873"
           this.status_data.WifiInformation.AccessPoint = "38:ED:18:E6:23"

           this.status_data.GatewayInformation.ETMSClient= "Connected"
           this.status_data.GatewayInformation.MDMClient= "Connected"

           this.status_data.RouteInformation.ATTRoute="Connected"
           this.status_data.RouteInformation.VerizonRoute="Connected"
           this.status_data.RouteInformation.WifiRoute="Not connected"
           this.status_data.RouteInformation.MHzRadio="Connected"

           this.status_data.RadioInformation.RadioId="10944515"
           this.status_data.RadioInformation.EMPAddress="netx. TNS_ELM+0.I. ant k.ant k+63. Radio o220"

            this.tabName = "Locomotive System Status"
           },
    }
});