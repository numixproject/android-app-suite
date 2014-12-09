
$(document).ready(function(){  

function toggleDialog(transition) {
      var dialog = document.
        querySelector('paper-dialog[transition=' + transition + ']');
      dialog.toggle();
    }
var button = document.getElementById('download');

button.addEventListener('click', function(event) {
toggleDialog('core-transition-center')
});
});