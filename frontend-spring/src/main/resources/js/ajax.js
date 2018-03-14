(function($){
    var $form = $('#urlForm');
    $form.on('submit', function(e) {
        e.preventDefault();
        $.ajax({
            url: $form.attr('action'),
            type: 'post',
            data: $form.serialize(),
            success: function(data) {
                console.log('Submission was successful.');
                console.log(data);
            },
            error: function (data) {
                console.log('An error occurred.');
                console.log(data);
            }
        });
    });
});