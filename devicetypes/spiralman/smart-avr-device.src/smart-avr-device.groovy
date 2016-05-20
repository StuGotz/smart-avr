metadata {
  definition (name: "Smart AVR Device", namespace: "spiralman", author: "Thomas Stephens") {
    capability "Switch"
  }

  preferences {
    input "receiverIp", "string", title: "Receiver IP address",
               description: "IP Address of Receiver (not proxy)",
               required: true, displayDuringSetup: true
  }

  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    standardTile("switch", "device.switch", width: 1, height: 1,
                 canChangeIcon: true) {
      state "off", label: '${name}', action: "switch.on",
                                       icon: "st.switches.switch.off",
                                       backgroundColor: '#ffffff'
      state "on", label: '${name}', action: "switch.off",
                                       icon: "st.switches.switch.on",
                                       backgroundColor: '#79b821'
    }

    main "switch"
    details "switch"
  }
}

def getProxyIp() {
  getDataValue("ip")
}

def getProxyPort() {
  return getDataValue("port")
}

// parse events into attributes
def parse(String description) {
  def msg = parseLanMessage(description)

  msg.body.eachLine { line ->
    if (line.startsWith('PW')) {
      def avrState = line.substring(2)
      def switchState

      if (avrState == 'ON') {
        switchState = 'on'
      }
      else if (avrState == 'STANDBY') {
        switchState = 'off'
      }

      log.debug "The receiver is ${switchState}"
      return createEvent(name: 'switch', value: switchState)
  }
}

def sync(ip, port) {
  def existingIp = getDataValue("ip")
  def existingPort = getDataValue("port")
  if (ip && ip != existingIp) {
    updateDataValue("ip", ip)
  }
  if (port && port != existingPort) {
    updateDataValue("port", port)
  }
}

def _avrCommand(command) {
  def result = new physicalgraph.device.HubAction(method: "GET",
                                                  path: "/avr/command",
                                                  headers: [
                                                    HOST: getHostAddress()
                                                  ],
                                                  query: [cmd: command]
                                                 )
  return result
}

def on() {
  return _avrCommand("PWON")
}

def off() {
  return _avrCommand("PWSTANDBY")
}

// Just copy pasted from SmartThings docs :-(
// gets the address of the device
private getHostAddress() {
  def ip = getDataValue("ip")
  def port = getDataValue("port")

  if (!ip) {
    def parts = device.deviceNetworkId.split(":")
    if (parts.length == 2) {
      ip = parts[0]
      port = parts[1]
    } else {
      log.warn "Can't figure out ip and port for device: ${device.id}"
    }
  }

  log.debug "Using IP: $ip and port: $port for device: ${device.id}"
  return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
  return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
  return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
