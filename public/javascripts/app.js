$(function(){


    function draw(tree) {
        $('#tree').jstree();
        $('#tree').jstree(true).settings.core.data = tree;
        $('#tree').jstree(true).refresh();
    }

    function getTreeModel(tree) {
        var children = [];
        for(var c in tree.children) {
            children.push(getTreeModel(tree.children[c]));
        }

        return { text: tree.rule + ': ' + tree.text, children: children, state: { opened: true } }
    }

    var url = 'api/parse';
    $('#parse').click(function() {
        var grammar = $('#grammar').val();
        var src = $('#src').val();
        var startRuleSel = $('#startRule');
        $.post(url, { grammar: grammar, src: src, rule: startRuleSel.val() }, function(data){
            startRuleSel.empty();
            for(var o in data.rules) {
                startRuleSel.append('<option>' + data.rules[o] + '</option>');
            }

            draw(getTreeModel(data.tree));

            $('#grammarError').hide();
        }).fail(function(err){
            $('#grammarError').show();
        });
    });
})