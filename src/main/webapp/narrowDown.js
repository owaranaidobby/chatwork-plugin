var ChatworkPublisher = {
    resetNarrowDown: function(){
        var select = ChatworkPublisher.roomIdSelect();
        if(!select){
            return;
        }
        for(var i = 0; i != select.options.length; i++){
            var option = select.options[i];
            option.style.removeProperty("display");
        }
    },
    narrowDown: function(type){
        var select = ChatworkPublisher.roomIdSelect();
        if(!select){
            return;
        }
        var prefix = "[" + type + "]";
        for(var i = 0; i != select.options.length; i++){
            var option = select.options[i];
            if(option.innerText.startsWith(prefix)){
                option.style.removeProperty("display");
            } else {
                option.style.display = "none";
            }
        }
    },
    roomIdSelect: function(){
        var fillpath = "com.vexus2.jenkins.chatwork.jenkinschatworkplugin.ChatworkPublisher/fillRidItems";
        var selects = document.getElementsByTagName("select");
        for(var i = 0; i != selects.length; i++){
            var fillurl = selects[i].getAttribute("fillurl") || "";
            if(fillurl.endsWith(fillpath)){
                return selects[i];
            }
        }
        return null;
    }
};
