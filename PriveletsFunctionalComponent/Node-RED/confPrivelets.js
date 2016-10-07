module.exports = function(RED) {
    function confPrivelets(config) {
        RED.nodes.createNode(this,config);
	 var node = this;
        this.on('input', function(msg) {
           	msg.payload=msg.payload;
			msg.Interval=config.Interval;
	        msg.Public=config.Public;
		    msg.LowPrivacy=config.LowPrivacy;
		    msg.MediumPrivacy=config.MediumPrivacy;
			msg.HighPrivacy=config.HighPrivacy;
			msg.Private=config.Private;
            node.send(msg);
        });
    }
    RED.nodes.registerType("confPrivelets",confPrivelets);
}
