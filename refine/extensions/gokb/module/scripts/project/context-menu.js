GOKb.contextMenu = {
  applyRules : function (menu, name) {
    $.each(GOKb.contextMenu.rules[name], function(){
      menu.applyrule(this);
    });
  },
  disableMenu : function () {
    var currentEl = $(document.activeElement);
    if (!currentEl.is('.select2-input')) {
      switch (currentEl.prop("tagName")) {
        case "INPUT" :
        case "TEXTAREA" :
          
          return true;
      }
    }
    // Disable the menu by default.
    return false;
  },
  disableOptions : function (menu) {
    var currentEl = $(document.activeElement);
    if (!currentEl.is('.select2-input')) {
      switch (currentEl.prop("tagName")) {
        case "INPUT" :
        case "TEXTAREA" :
          GOKb.contextMenu.applyRules(menu, 'enable-lookup');
          break;
      }
    }

    // We are not in an element that allows text entry.
    GOKb.contextMenu.applyRules(menu, 'disable-lookup');
  },
};

GOKb.contextMenu.rules = {
  "disable-lookup" : [
    { disable: true, items: ["gokb-lookup"] }
  ],
  "enable-lookup" : [
    { disable: false, items: [
      "gokb-lookup",
      "gokb-lookup-org",
      "gokb-lookup-package",
      "gokb-lookup-platform"]
    }
  ]
};

GOKb.contextMenu.options = {
  width: 150,
  items: [
    {
      text: "GOKb Lookup",
//      icon: "",
      alias: "gokb-lookup",
      type:"group",
      width: 150,
      items:[
        {
          text: "Organisation",
//          icon: "",
          alias: "gokb-lookup-org",
          action: function () {
            GOKb.handlers.lookup ($(document.activeElement), "org", ["variantNames.variantName"], ["variantNames.variantName"]);
          }
        },
        {
          text: "Package",
//          icon: "",
          alias: "gokb-lookup-package",
          action: function () {
            GOKb.handlers.lookup ($(document.activeElement), "package", ["variantNames.variantName"], ["variantNames.variantName"], true);
          }
        },
        {
          text: "Platform",
//          icon: "",
          alias: "gokb-lookup-platform",
          action: function () {
            GOKb.handlers.lookup ($(document.activeElement), "platform", ["variantNames.variantName"], ["variantNames.variantName"]);
          }
        },
      ]
    },
  ],
  onShow: GOKb.contextMenu.disableOptions,
  onContextMenu: GOKb.contextMenu.disableMenu,
};

// Add the context menu here.
$(document).ready(function(){
  $("body").contextmenu(GOKb.contextMenu.options);
});