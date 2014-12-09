//= require jquery
//= require bootstrap
//= require ZeroClipboard.Core
//= require ZeroClipboard

(function($) {
  $(function() {
    $("button[data-clipboard-target]").each(function() {
      var clip = new ZeroClipboard(this);
      clip.on( "ready", function() {
        clip.on("aftercopy", function() {
          alert("Text to clipboard was copied.");
        })
      });
    });
  });
})(jQuery);
