$(document).ready(
        function(){

            $('.add-user-to-app-btn').click(function(){
                $('.select-role-row').remove();
                $('#add-user-table td').removeClass('color-borders');
                $(this).parent().parent().parent().find('>:last-child').addClass("color-borders");
                $(this).parent().parent().parent().before('<tr class="select-role-row"><td colspan="2" class="color-borders"><div class="pull-right span4">' +
                        '<h4>Select a role for '+$(this).attr('data-value')+'</h4>'+
                        '<label class="radio">' +
                        '<input type="radio" name="optionsRadios" value="option1" checked="checked">' +
                        'Developer' +
                        '</label>' +
                        '<label class="radio">' +
                        '<input type="radio" name="optionsRadios" value="option2">' +
                        'QA' +
                        '</label>' +
                        '<label class="radio">' +
                        '<input type="radio" name="optionsRadios" value="option2">' +
                        'DevOps' +
                        '</label>' +
                        '<button class="btn"><i class="icon-circle-arrow-up"></i> Add User</button>'+
                        '</div></td></tr>'); //the parent tr
            })
        }
);