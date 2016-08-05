$(function(){

    var parseUrl = 'api/parse/';
    var loadUrl = 'api/load/';
    var saveUrl = 'api/save/';

    function loadRules(rules, rule) {
        var startRuleSel = $('#startRule');
        startRuleSel.empty();
        for(var o in rules) {
            startRuleSel.append('<option>' + rules[o] + '</option>');
        }

        startRuleSel.val(rule);
    }

    function setErrors(errors) {
        var errorNotifications = errors.map(function(e) {
        return {
            column: e.col,
            row: e.line - 1,
            type: "error",
            text: e.message
        }});

        editor.getSession().setAnnotations(errorNotifications);
    }

    function parseExpression(url, callback) {
        var grammar = editor.getValue();
        var src = $('#src').val();
        var startRuleSel = $('#startRule');
        $.post(url, { grammar: grammar, src: src, rule: startRuleSel.val() }, function(data){
            if (data.tree && data.rules) {
                loadRules(data.rules, data.rule);
                draw(getTreeModel(data.tree));
                $('#grammarError').hide();
                setErrors([]);
            } else {
                $('#grammarError').show();
                setErrors(data.errors);
                draw(null);
            }

            if (callback) {
                callback(data.id);
            }

        }).fail(function(err){
            $('#grammarError').show();
        });
    }

    function loadTree(id) {
        $.get(loadUrl + id, {}, function(res){
            editor.setValue(res.grammar);
            $('#src').val(res.src);

            if (res.tree) {
                draw(getTreeModel(JSON.parse(res.tree)));
            }

            loadRules(res.rules.split(','), res.rule);
        });
    }

    var sessionId = location.hash.substring(1);
    if (sessionId) {
        loadTree(sessionId);
    }

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

    $('#parse').click(function() {
        parseExpression(parseUrl);
    });

    $('#save').click(function() {
        parseExpression(saveUrl, function(id){
            window.location = '/#' + id;
        });
    });
})