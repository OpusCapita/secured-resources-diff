//= require jquery
//= require ZeroClipboard.Core
//= require ZeroClipboard
//= require select2.full.min

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
    $("select").select2();
  });
})(jQuery);
